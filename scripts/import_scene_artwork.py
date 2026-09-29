#!/usr/bin/env python3
"""Upload an explicitly reviewed route-scene manifest; dry-run is the default.

The management token is read only from SCENE_MANAGEMENT_TOKEN, never CLI arguments.
This tool neither publishes a route version nor changes any personal itinerary.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import sys
from urllib.error import HTTPError
from urllib.parse import quote, urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener


MAX_BYTES = 10 * 1024 * 1024


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError('服务返回重定向，已停止；请直接使用正确的 API 地址')


def validate_api_base(value):
    parts = urlsplit(value)
    if (parts.scheme not in ('http', 'https') or not parts.hostname or
            parts.username or parts.password or parts.query or parts.fragment):
        raise ValueError('API 地址必须为不含凭据、查询或片段的 HTTP(S) 地址')
    return value.rstrip('/')


def text(value, name):
    if not isinstance(value, str) or not value.strip() or value != value.strip():
        raise ValueError(f'{name} 必须为明确的非空文本')
    return value


def inspect_image(path):
    path = path.resolve(strict=True)
    if not path.is_file() or not 0 < path.stat().st_size <= MAX_BYTES:
        raise ValueError(f'图片必须非空且不超过 10 MiB：{path.name}')
    content = path.read_bytes()
    if content.startswith(b'\x89PNG\r\n\x1a\n'):
        content_type = 'image/png'
    elif content.startswith(b'\xff\xd8\xff'):
        content_type = 'image/jpeg'
    else:
        raise ValueError(f'仅支持 PNG / JPEG：{path.name}')
    # Full image decoding and pixel bounds are validated by the service.
    return {'file': str(path), 'mediaId': hashlib.sha256(content).hexdigest(),
            'bytes': len(content), 'contentType': content_type}


def load_manifest(path):
    path = Path(path).resolve(strict=True)
    data = json.loads(path.read_text(encoding='utf-8'))
    allowed = {'routeId', 'routeVersionId', 'overviewFile', 'days'}
    if not isinstance(data, dict) or set(data) - allowed:
        raise ValueError('manifest 存在未定义字段')
    plan = {'routeId': text(data.get('routeId'), 'routeId'),
            'routeVersionId': text(data.get('routeVersionId'), 'routeVersionId'),
            'days': []}
    if 'overviewFile' in data:
        plan['overview'] = inspect_image(path.parent / text(data['overviewFile'], 'overviewFile'))
    if not isinstance(data.get('days'), list):
        raise ValueError('days 必须为数组；清空时显式提供 []')
    seen = set()
    for day in data['days']:
        if not isinstance(day, dict) or set(day) != {'referenceDayId', 'file'}:
            raise ValueError('每日项必须明确提供 referenceDayId 和 file')
        identity = text(day['referenceDayId'], 'referenceDayId')
        if identity in seen:
            raise ValueError('参考日身份重复')
        seen.add(identity)
        plan['days'].append({'referenceDayId': identity,
                             'image': inspect_image(path.parent / text(day['file'], 'file'))})
    return plan


def verify_public_detail(plan, detail):
    version = detail.get('currentVersion', {})
    if (detail.get('routeId') != plan['routeId'] or
            version.get('versionId') != plan['routeVersionId']):
        raise ValueError('公开路线或版本已变化，请重新核对 manifest')
    identities = {day.get('identity') for day in version.get('referenceDays', [])}
    if any(day['referenceDayId'] not in identities for day in plan['days']):
        raise ValueError('manifest 的参考日不属于指定公开版本')


class Client:
    def __init__(self, base, token=None):
        self.base = validate_api_base(base)
        self.token = token
        self.opener = build_opener(NoRedirect())

    def request(self, path, method='GET', body=None, content_type=None, management=False):
        headers = {'Accept': 'application/json'}
        if management:
            if not self.token:
                raise ValueError('写入或管理读取需要 SCENE_MANAGEMENT_TOKEN 环境变量')
            headers['X-Scene-Management-Token'] = self.token
        if content_type:
            headers['Content-Type'] = content_type
        request = Request(self.base + path, data=body, headers=headers, method=method)
        try:
            with self.opener.open(request, timeout=60) as response:
                result = json.load(response)
        except HTTPError as error:
            # Avoid echoing response bodies or request headers containing secrets.
            raise ValueError(f'服务请求失败 HTTP {error.code}，未继续后续步骤') from None
        if not isinstance(result, dict) or 'data' not in result:
            raise ValueError('服务没有返回有效的 data 封套')
        return result['data']


def route_path(plan):
    return '/public-routes/' + quote(plan['routeId'], safe='')


def apply_manifest(client, plan, expected_revision):
    if isinstance(expected_revision, bool) or not isinstance(expected_revision, int) or expected_revision < 0:
        raise ValueError('--apply 必须显式提供非负 --expected-revision')
    verify_public_detail(plan, client.request(route_path(plan)))
    path = ('/scene-management/routes/' + quote(plan['routeId'], safe='') +
            '/versions/' + quote(plan['routeVersionId'], safe=''))
    current = client.request(path, management=True)
    if current.get('revision') != expected_revision:
        raise ValueError('场景 revision 已变化，尚未上传；请核对已有绑定后重试')
    images = ([plan['overview']] if 'overview' in plan else []) + [d['image'] for d in plan['days']]
    uploaded = set()
    for image in images:
        if image['mediaId'] in uploaded:
            continue
        content = Path(image['file']).read_bytes()
        if hashlib.sha256(content).hexdigest() != image['mediaId']:
            raise ValueError('图片在预检后变化，已停止')
        result = client.request('/scene-management/media', 'POST', content,
                                image['contentType'], management=True)
        if result.get('mediaId') != image['mediaId']:
            raise ValueError('上传返回的内容哈希与原图不一致，未绑定')
        uploaded.add(image['mediaId'])
    payload = {'expectedRevision': expected_revision,
               'days': [{'referenceDayId': day['referenceDayId'], 'mediaId': day['image']['mediaId']}
                        for day in plan['days']]}
    if 'overview' in plan:
        payload['overviewMediaId'] = plan['overview']['mediaId']
    result = client.request(path, 'PUT', json.dumps(payload).encode('utf-8'),
                            'application/json', management=True)
    public = client.request(route_path(plan) + '/scenes?routeVersionId=' + quote(plan['routeVersionId'], safe=''))
    if public != result.get('projection'):
        raise ValueError('绑定已提交，但公共读取与提交结果不一致；请读取核对，不要盲目重试')
    return result


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--manifest', required=True, type=Path)
    parser.add_argument('--api-base-url', required=True, help='完整 API 根地址，通常以 /walkbg/api/v1 结尾')
    parser.add_argument('--apply', action='store_true', help='执行上传和绑定，默认仅预检')
    parser.add_argument('--expected-revision', type=int)
    args = parser.parse_args(argv)
    try:
        plan = load_manifest(args.manifest)
        client = Client(args.api_base_url, os.environ.get('SCENE_MANAGEMENT_TOKEN'))
        if args.apply:
            result = apply_manifest(client, plan, args.expected_revision)
            print(json.dumps({'applied': True, 'result': result}, ensure_ascii=False, indent=2))
        else:
            verify_public_detail(plan, client.request(route_path(plan)))
            print(json.dumps({'applied': False, 'plan': plan}, ensure_ascii=False, indent=2))
        return 0
    except (ValueError, OSError) as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
