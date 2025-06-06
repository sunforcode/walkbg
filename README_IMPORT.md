# WalkBG 数据导入工具使用说明

## 📋 **概述**

本工具用于将JSON格式的路线数据导入到WalkBG服务中。提供了Python和Bash两种版本的导入脚本。

## 🛠 **准备工作**

### 1. 确保服务运行
\`\`\`bash
# 启动WalkBG服务
mvn spring-boot:run

# 或者
java -jar target/walkbg-*.jar
\`\`\`

### 2. 验证服务状态
\`\`\`bash
curl http://localhost:8080/walkbg/api/users?page=0&size=1
\`\`\`

## 🚀 **使用方法**

### **方法1: 使用Python脚本（推荐）**

#### 安装依赖
\`\`\`bash
pip install requests
\`\`\`

#### 运行脚本
\`\`\`bash
python import_routes.py routes.json
\`\`\`

#### 功能特点
- ✅ 自动创建用户（如果不存在）
- ✅ 智能数据转换和映射
- ✅ 详细的进度显示和错误处理
- ✅ 支持批量导入多条路线
- ✅ 完整的统计报告

### **方法2: 使用Bash脚本（快速测试）**

\`\`\`bash
./import_routes.sh
\`\`\`

#### 功能特点
- ✅ 快速测试导入功能
- ✅ 创建示例用户和路线
- ✅ 验证导入结果
- ✅ 无需额外依赖

## 📊 **数据格式说明**

### **JSON数据结构**
\`\`\`json
[{
  "id": "route_001",
  "name": "路线名称",
  "description": "路线描述",
  "region": "地区",
  "difficulty": 1,
  "waypoints": [...],
  "segments": [...],
  "created_by": "user_001"
}]
\`\`\`

### **字段映射关系**

| JSON字段 | API字段 | 类型 | 说明 |
|----------|---------|------|------|
| `id` | `id` | String | 路线ID |
| `name` | `name` | String | 路线名称 |
| `description` | `description` | String | 路线描述 |
| `region` | `region` | String | 地区 |
| `difficulty` | `difficulty` | Int | 难度等级 |
| `segments[].distance` | `distance` | BigDecimal | 总距离(计算) |
| `segments[].duration` | `duration` | Int | 总时长(计算) |
| `waypoints[0].latitude` | `latitude` | BigDecimal | 起点纬度 |
| `waypoints[0].longitude` | `longitude` | BigDecimal | 起点经度 |
| `waypoints[0].elevation` | `altitude` | BigDecimal | 起点海拔 |
| `segments[].elevation_gain` | `elevationGain` | BigDecimal | 总爬升(计算) |
| `segments[].elevation_loss` | `elevationLoss` | BigDecimal | 总下降(计算) |
| `status` | `status` | Int | 状态映射 |
| `popularity` | `popularity` | Int | 热度值 |
| `created_by` | `createdBy` | String | 创建者ID |

## 🔧 **自定义配置**

### **修改服务地址**
\`\`\`python
# 在import_routes.py中修改
BASE_URL = "http://your-server:port/walkbg/api"
\`\`\`

\`\`\`bash
# 在import_routes.sh中修改
BASE_URL="http://your-server:port/walkbg/api"
\`\`\`

### **修改数据映射**
在`convert_route_data`方法中调整字段映射逻辑：

\`\`\`python
def convert_route_data(self, route_json: Dict) -> Dict:
    # 自定义数据转换逻辑
    route_data = {
        "id": route_json.get('id'),
        "name": route_json.get('name'),
        # 添加更多字段映射...
    }
    return route_data
\`\`\`

## 📝 **使用示例**

### **1. 导入单条路线**
\`\`\`bash
# 创建包含单条路线的JSON文件
echo '[{"id":"test_001","name":"测试路线","created_by":"user_001"}]' > test_route.json

# 导入
python import_routes.py test_route.json
\`\`\`

### **2. 批量导入**
\`\`\`bash
# 导入完整的routes.json文件
python import_routes.py routes.json
\`\`\`

### **3. 验证导入结果**
\`\`\`bash
# 查看用户
curl "http://localhost:8080/walkbg/api/users?page=0&size=10"

# 查看路线
curl "http://localhost:8080/walkbg/api/routes?page=0&size=10"

# 查看特定路线
curl "http://localhost:8080/walkbg/api/routes/route_001"
\`\`\`

## 🐛 **故障排除**

### **常见问题**

#### 1. 服务连接失败
\`\`\`
❌ 服务连接异常: Connection refused
\`\`\`
**解决方案**: 确保WalkBG服务正在运行，检查端口和地址配置

#### 2. 用户创建失败
\`\`\`
❌ 创建用户失败: 400 - Bad Request
\`\`\`
**解决方案**: 检查用户数据格式，确保必填字段完整

#### 3. 路线创建失败
\`\`\`
❌ 创建路线失败: 400 - Bad Request
\`\`\`
**解决方案**: 检查路线数据格式，确保数据类型正确

#### 4. JSON解析错误
\`\`\`
❌ 加载JSON文件失败: Expecting ',' delimiter
\`\`\`
**解决方案**: 检查JSON文件格式，使用JSON验证工具验证

### **调试模式**

在Python脚本中启用详细日志：
\`\`\`python
import logging
logging.basicConfig(level=logging.DEBUG)
\`\`\`

## 📈 **性能优化**

### **大批量数据导入**
- 使用批量API（如果支持）
- 添加进度保存和断点续传
- 并发导入（注意API限制）

### **数据预处理**
- 验证JSON格式
- 清理无效数据
- 数据去重

## 🔒 **安全注意事项**

- 不要在生产环境中使用默认的用户凭据
- 确保API访问权限正确配置
- 备份现有数据后再进行导入操作

## 📞 **技术支持**

如果遇到问题，请检查：
1. 服务日志：`logs/walkbg.log`
2. 数据库状态：访问H2控制台
3. API文档：`http://localhost:8080/walkbg/swagger-ui.html`

---

**版本**: v1.0  
**更新时间**: 2024-06-06