# WalkBG API 脚本工具

这个目录包含了用于调用WalkBG API的各种shell脚本工具。

## 📋 脚本列表

1. **create_user.sh** - 创建单个用户
2. **create_test_users.sh** - 批量创建测试用户
3. **create_route.sh** - 创建完整路线
4. **create_simple_route.sh** - 创建简单路线

## 🚀 快速开始

### 1. 创建测试用户
```bash
./scripts/create_test_users.sh
```

### 2. 创建路线

#### 方法1: 创建简单路线（推荐）
```bash
# 使用默认参数
./scripts/create_simple_route.sh

# 自定义路线名称
./scripts/create_simple_route.sh "我的徒步路线"

# 指定创建者
./scripts/create_simple_route.sh "我的徒步路线" "用户ID"
```

#### 方法2: 创建完整路线
```bash
# 创建包含路点、路段等完整信息的路线
./scripts/create_complete_route.sh

# 自定义路线名称
./scripts/create_complete_route.sh "华山西峰路线"
```

### 3. 验证结果
```bash
# 查看所有路线
curl -X GET "http://localhost:8080/walkbg/api/routes" | jq .

# 查看路线地图数据
curl -X GET "http://localhost:8080/walkbg/api/route-map-data" | jq .
```

## 📖 详细说明

### create_user.sh - 创建用户脚本

**功能**: 创建单个用户

**使用方法**:
```bash
./scripts/create_user.sh <username> <email> [nickname] [phone]
```

**示例**:
```bash
./scripts/create_user.sh john_doe john@example.com "John Doe" "13800138000"
```

### create_test_users.sh - 批量创建测试用户

**功能**: 批量创建8个预定义测试用户

**使用方法**:
```bash
./scripts/create_test_users.sh
```

**创建的用户**:
- admin (系统管理员)
- hiking_expert (徒步专家)
- mountain_guide (山地向导)
- john_doe, alice, bob (普通用户)
- local_guide (本地向导)
- route_creator (路线创建者)

### create_route.sh - 创建完整路线

**功能**: 创建包含所有关联信息的完整路线

**使用方法**:
```bash
./scripts/create_route.sh [route_name] [creator_id]
```

**示例**:
```bash
./scripts/create_route.sh "泰山登顶路线" "user_123"
```

**包含数据**:
- 4个路点 (慈光阁、半山寺、玉屏楼、天都峰顶)
- 3个路段 (包含距离、海拔、时间)
- 2个补给点 (小卖部、茶水站)
- 1个营地 (露营区)
- 2个标记点 (景点、危险区域)
- 1个日程计划
- 路线标签和图片

### create_simple_route.sh - 创建简单路线

**功能**: 创建只包含基本信息的路线，用于测试

**使用方法**:
```bash
./scripts/create_simple_route.sh [route_name] [creator_id]
```

## 🔧 故障排除

### 常见错误

#### HTTP 500 服务器内部错误
**原因**: 服务器问题或用户不存在
**解决**:
```bash
# 检查服务器状态
curl -s "http://localhost:8080/walkbg/api/users" | jq .

# 使用有效用户ID
USER_ID=$(curl -s "http://localhost:8080/walkbg/api/users" | jq -r '.data.content[0].id')
./scripts/create_route.sh "测试路线" "$USER_ID"
```

#### HTTP 404 用户不存在
**解决**:
```bash
# 先创建用户
./scripts/create_user.sh test_user test@example.com

# 再创建路线
./scripts/create_route.sh "新路线" "test_user_id"
```

### 验证结果

#### 查看用户
```bash
curl -X GET "http://localhost:8080/walkbg/api/users" | jq .
```

#### 查看路线
```bash
curl -X GET "http://localhost:8080/walkbg/api/routes" | jq .
```

## ⚙️ 配置

### 服务器地址
默认: `http://localhost:8080/walkbg`

### 依赖工具
- `curl` - HTTP请求 (必需)
- `jq` - JSON处理 (可选)
- `uuidgen` - UUID生成 (可选)

## 📝 注意事项

1. 确保WalkBG服务正在运行
2. 用户名和邮箱必须唯一
3. 创建路线前确保用户存在
4. 脚本会自动生成UUID
5. 所有用户默认为激活状态
