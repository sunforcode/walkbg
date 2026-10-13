import importlib.util
import json
from pathlib import Path
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / 'import_scene_artwork.py'
spec = importlib.util.spec_from_file_location('import_scene_artwork', SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class SceneImportTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / 'scene.png').write_bytes(b'\x89PNG\r\n\x1a\n' + b'test-image')
        self.manifest = {
            'routeId': 'route-1', 'routeVersionId': 'version-1',
            'overviewFile': 'scene.png',
            'days': [{'referenceDayId': 'reference-day-a', 'file': 'scene.png'}],
        }

    def bundle(self):
        path = self.root / 'manifest.json'
        path.write_text(json.dumps(self.manifest))
        return module.load_manifest(path)

    def test_manifest_preserves_explicit_day_identity_and_hashes_files(self):
        plan = self.bundle()
        self.assertEqual(plan['days'][0]['referenceDayId'], 'reference-day-a')
        self.assertEqual(len(plan['overview']['mediaId']), 64)
        self.assertEqual(plan['overview']['mediaId'], plan['days'][0]['image']['mediaId'])

    def test_duplicate_reference_day_is_rejected_before_requests(self):
        self.manifest['days'] *= 2
        with self.assertRaisesRegex(ValueError, '重复'):
            self.bundle()

    def test_day_numbers_do_not_substitute_for_explicit_identity(self):
        self.manifest['days'] = [{'dayNumber': 1, 'file': 'scene.png'}]
        with self.assertRaises(ValueError):
            self.bundle()

    def test_version_change_is_rejected_before_upload(self):
        plan = self.bundle()
        with self.assertRaisesRegex(ValueError, '版本'):
            module.verify_public_detail(plan, {
                'routeId': 'route-1',
                'currentVersion': {'versionId': 'version-2', 'referenceDays': []},
            })

    def test_unknown_reference_day_is_rejected(self):
        plan = self.bundle()
        with self.assertRaisesRegex(ValueError, '参考日'):
            module.verify_public_detail(plan, {
                'routeId': 'route-1',
                'currentVersion': {'versionId': 'version-1', 'referenceDays': []},
            })

    def test_stale_revision_stops_before_any_upload_or_write(self):
        plan = self.bundle()
        calls = []

        class FakeClient:
            def request(self, path, method='GET', **kwargs):
                calls.append((path, method))
                if path.startswith('/public-routes/'):
                    return {'routeId': 'route-1', 'currentVersion': {
                        'versionId': 'version-1',
                        'referenceDays': [{'identity': 'reference-day-a'}],
                    }}
                return {'revision': 3}

        with self.assertRaisesRegex(ValueError, 'revision'):
            module.apply_manifest(FakeClient(), plan, expected_revision=0)
        self.assertEqual(len(calls), 2)
        self.assertTrue(all(method == 'GET' for _, method in calls))

    def test_apply_without_explicit_revision_makes_no_requests(self):
        with self.assertRaisesRegex(ValueError, 'expected-revision'):
            module.apply_manifest(None, self.bundle(), expected_revision=None)

    def test_redirects_do_not_forward_management_credentials(self):
        with self.assertRaisesRegex(ValueError, '重定向'):
            module.NoRedirect().redirect_request(None, None, 302, 'Found', {}, 'https://other.example')

    def test_api_base_rejects_embedded_credentials_and_query(self):
        for url in ['https://user:pass@example.com/api/v1', 'https://example.com/api/v1?token=x', 'file:///tmp']:
            with self.assertRaises(ValueError):
                module.validate_api_base(url)


if __name__ == '__main__':
    unittest.main()
