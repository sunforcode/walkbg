#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
WalkBG 路线数据导入脚本
从JSON文件读取路线数据并通过API导入到服务中
"""

import json
import requests
import sys
from datetime import datetime
from typing import Dict, List, Any
import uuid

# 服务配置
BASE_URL = "http://localhost:8080/walkbg/api"
HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json"
}

class RouteImporter:
    def __init__(self, base_url: str = BASE_URL):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update(HEADERS)
        
    def load_json_data(self, file_path: str) -> List[Dict]:
        """从JSON文件加载数据"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            print(f"✅ 成功加载JSON数据，共 {len(data)} 条路线")
            return data
        except Exception as e:
            print(f"❌ 加载JSON文件失败: {e}")
            return []
    
    def create_user_if_not_exists(self, user_id: str) -> bool:
        """创建用户（如果不存在）"""
        try:
            # 检查用户是否存在
            response = self.session.get(f"{self.base_url}/users/{user_id}")
            if response.status_code == 200:
                print(f"✅ 用户 {user_id} 已存在")
                return True
            
            # 创建用户
            user_data = {
                "id": user_id,
                "username": f"user_{user_id}",
                "email": f"{user_id}@example.com",
                "nickname": f"用户{user_id}",
                "isActive": True
            }
            
            response = self.session.post(f"{self.base_url}/users", json=user_data)
            if response.status_code == 201:
                print(f"✅ 成功创建用户 {user_id}")
                return True
            else:
                print(f"❌ 创建用户失败: {response.status_code} - {response.text}")
                return False
                
        except Exception as e:
            print(f"❌ 用户操作异常: {e}")
            return False
    
    def convert_route_data(self, route_json: Dict) -> Dict:
        """将JSON路线数据转换为API格式"""
        # 计算总距离和时长
        total_distance = sum(seg.get('distance', 0) for seg in route_json.get('segments', []))
        total_duration = sum(self.parse_duration(seg.get('duration', '0:00')) for seg in route_json.get('segments', []))
        
        # 获取起点和终点坐标
        waypoints = route_json.get('waypoints', [])
        start_point = waypoints[0] if waypoints else {}
        end_point = waypoints[-1] if len(waypoints) > 1 else start_point
        
        # 计算总爬升和下降
        total_elevation_gain = sum(seg.get('elevation_gain', 0) for seg in route_json.get('segments', []))
        total_elevation_loss = sum(seg.get('elevation_loss', 0) for seg in route_json.get('segments', []))
        
        route_data = {
            "id": route_json.get('id', str(uuid.uuid4())),
            "name": route_json.get('name', ''),
            "description": route_json.get('description', ''),
            "region": route_json.get('region', ''),
            "distance": total_distance,
            "duration": total_duration,
            "latitude": start_point.get('latitude'),
            "longitude": start_point.get('longitude'),
            "altitude": start_point.get('elevation'),
            "elevationGain": total_elevation_gain,
            "elevationLoss": total_elevation_loss,
            "difficulty": route_json.get('difficulty', 1),
            "routeType": self.determine_route_type(waypoints),
            "status": 1 if route_json.get('status') == 'completed' else 0,
            "popularity": route_json.get('popularity', 0),
            "createdBy": route_json.get('created_by', 'system'),
            "coverUrl": route_json.get('cover_url'),
            "mapDataId": route_json.get('map_data_id'),
            "defaultMapId": route_json.get('default_map_id', '')
        }
        
        return route_data
    
    def parse_duration(self, duration_str: str) -> int:
        """解析时长字符串为小时数"""
        try:
            if ':' in duration_str:
                parts = duration_str.split(':')
                hours = int(parts[0])
                minutes = int(parts[1]) if len(parts) > 1 else 0
                return hours + (minutes / 60)
            else:
                return float(duration_str)
        except:
            return 0
    
    def determine_route_type(self, waypoints: List[Dict]) -> int:
        """根据路径点确定路线类型"""
        if len(waypoints) < 2:
            return 2  # 单程
        
        start = waypoints[0]
        end = waypoints[-1]
        
        # 如果起点和终点相同或很接近，认为是环线
        if (abs(start.get('latitude', 0) - end.get('latitude', 0)) < 0.001 and 
            abs(start.get('longitude', 0) - end.get('longitude', 0)) < 0.001):
            return 1  # 环线
        else:
            return 0  # 往返
    
    def create_route(self, route_data: Dict) -> bool:
        """创建路线"""
        try:
            response = self.session.post(f"{self.base_url}/routes", json=route_data)
            if response.status_code == 201:
                print(f"✅ 成功创建路线: {route_data['name']}")
                return True
            else:
                print(f"❌ 创建路线失败: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            print(f"❌ 创建路线异常: {e}")
            return False
    
    def create_waypoints(self, route_id: str, waypoints: List[Dict]) -> bool:
        """创建路径点（如果有对应的API）"""
        # 注意：当前的API可能没有单独的waypoint接口
        # 这里只是示例，实际需要根据你的API设计调整
        success_count = 0
        for wp in waypoints:
            waypoint_data = {
                "id": wp.get('id', str(uuid.uuid4())),
                "routeId": route_id,
                "name": wp.get('name', ''),
                "description": wp.get('description', ''),
                "latitude": wp.get('latitude'),
                "longitude": wp.get('longitude'),
                "elevation": wp.get('elevation'),
                "waypointType": wp.get('type', 'waypoint'),
                "sequenceNumber": wp.get('sequence_number', 0),
                "iconUrl": wp.get('icon_url'),
                "imageUrl": wp.get('image_url')
            }
            
            # 这里需要根据实际的waypoint API调整
            # response = self.session.post(f"{self.base_url}/waypoints", json=waypoint_data)
            success_count += 1
        
        print(f"✅ 路径点信息已准备: {success_count} 个点")
        return True
    
    def import_single_route(self, route_json: Dict) -> bool:
        """导入单条路线"""
        print(f"\n🚀 开始导入路线: {route_json.get('name', 'Unknown')}")
        
        # 1. 确保创建者用户存在
        created_by = route_json.get('created_by', 'system')
        if not self.create_user_if_not_exists(created_by):
            print(f"❌ 无法创建用户 {created_by}，跳过此路线")
            return False
        
        # 2. 转换路线数据
        route_data = self.convert_route_data(route_json)
        
        # 3. 创建路线
        if not self.create_route(route_data):
            return False
        
        # 4. 创建路径点（如果API支持）
        waypoints = route_json.get('waypoints', [])
        if waypoints:
            self.create_waypoints(route_data['id'], waypoints)
        
        print(f"✅ 路线导入完成: {route_data['name']}")
        return True
    
    def import_all_routes(self, json_file: str) -> None:
        """导入所有路线"""
        print("🎯 开始导入路线数据...")
        
        # 加载JSON数据
        routes_data = self.load_json_data(json_file)
        if not routes_data:
            return
        
        # 导入统计
        success_count = 0
        total_count = len(routes_data)
        
        # 逐个导入路线
        for i, route_json in enumerate(routes_data, 1):
            print(f"\n📍 进度: {i}/{total_count}")
            if self.import_single_route(route_json):
                success_count += 1
        
        # 输出结果
        print(f"\n🎉 导入完成!")
        print(f"✅ 成功: {success_count} 条")
        print(f"❌ 失败: {total_count - success_count} 条")
        print(f"📊 成功率: {success_count/total_count*100:.1f}%")
    
    def test_connection(self) -> bool:
        """测试服务连接"""
        try:
            response = self.session.get(f"{self.base_url}/users?page=0&size=1")
            if response.status_code == 200:
                print("✅ 服务连接正常")
                return True
            else:
                print(f"❌ 服务连接失败: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ 服务连接异常: {e}")
            return False

def main():
    """主函数"""
    print("🌟 WalkBG 路线数据导入工具")
    print("=" * 50)
    
    # 检查参数
    if len(sys.argv) != 2:
        print("使用方法: python import_routes.py <json_file_path>")
        print("示例: python import_routes.py routes.json")
        sys.exit(1)
    
    json_file = sys.argv[1]
    
    # 创建导入器
    importer = RouteImporter()
    
    # 测试连接
    if not importer.test_connection():
        print("❌ 无法连接到服务，请确保服务正在运行")
        sys.exit(1)
    
    # 开始导入
    importer.import_all_routes(json_file)

if __name__ == "__main__":
    main()