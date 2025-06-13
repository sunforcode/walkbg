# WalkBG 路线创建脚本使用说明

## 📋 **Route模型结构分析**

### **核心实体关系**

\`\`\`
Route (路线主表)
├── 基础信息字段
├── 地理位置信息
├── 难度和类型信息
└── 关联实体
    ├── Waypoint (路径点) - OneToMany
    ├── Segment (路段) - OneToMany  
    ├── RouteTag (标签) - OneToMany
    ├── RouteSeason (季节) - OneToMany
    ├── RouteImage (图片) - OneToMany
    ├── RouteRating (评分) - OneToOne
    └── User (创建者) - ManyToOne
\`\`\`

### **Route 主要字段**

| 字段名 | 类型 | 说明 | 示例值 |
|--------|------|------|--------|
| `id` | String | 路线唯一标识 | "route_001" |
| `name` | String | 路线名称 | "黄山天都峰徒步路线" |
| `description` | String | 路线描述 | "风景秀丽的高难度路线" |
| `region` | String | 地区名称 | "安徽黄山" |
| `regionId` | String | 地区ID | "region_huangshan_001" |
| `distance` | BigDecimal | 距离(km) | 12.5 |
| `duration` | Int | 预计用时(小时) | 8 |
| `latitude` | BigDecimal | 纬度 | 30.1394 |
| `longitude` | BigDecimal | 经度 | 118.1558 |
| `altitude` | BigDecimal | 海拔(m) | 800.0 |
| `elevationGain` | BigDecimal | 爬升(m) | 1064.0 |
| `elevationLoss` | BigDecimal | 下降(m) | 200.0 |
| `difficulty` | Int | 难度等级 | 0-3 (简单到极难) |
| `routeType` | Int | 路线类型 | 0:往返 1:环线 2:单程 3:多日 |
| `routeDirection` | Int | 路线方向 | 0:顺时针 1:逆时针 2:双向 |
| `status` | Int | 状态 | 0:规划中 1:已发布 2:已关闭 |
| `coverUrl` | String | 封面图片URL | "https://example.com/cover.jpg" |
| `createdBy` | String | 创建者ID | "user_admin_001" |
| `popularity` | Int | 热度值 | 自动计算 |

### **关联实体结构**

#### **Waypoint (路径点)**
\`\`\`json
{
  "name": "慈光阁",
  "description": "黄山南大门，徒步起点",
  "latitude": 30.1394,
  "longitude": 118.1558,
  "elevation": 800.0,
  "type": "起点",
  "sequence_number": 1
}
\`\`\`

#### **Segment (路段)**
\`\`\`json
{
  "distance": 3.2,
  "elevation_gain": 540.0,
  "elevation_loss": 0.0,
  "estimated_time": 2.5,
  "difficulty": 1,
  "terrain": "石阶路",
  "surface_type": "石阶"
}
\`\`\`

## 🚀 **脚本使用方法**

### **1. 完整版脚本 (create_route.sh)**

创建包含完整信息的复杂路线，包含多个路径点、路段和图片。

\`\`\`bash
# 运行完整版脚本
./create_route.sh
\`\`\`

**特点：**
- ✅ 完整的路线信息
- ✅ 多个路径点 (4个)
- ✅ 多个路段 (4个)
- ✅ 标签和季节信息
- ✅ 图片信息
- ✅ 详细的错误处理
- ✅ 彩色输出和状态检查

### **2. 简化版脚本 (create_simple_route.sh)**

创建基本的路线信息，用于快速测试。

\`\`\`bash
# 运行简化版脚本
./create_simple_route.sh
\`\`\`

**特点：**
- ✅ 基础路线信息
- ✅ 简单的路径点 (2个)
- ✅ 单个路段
- ✅ 快速测试

## 📝 **使用步骤**

### **准备工作**

1. **启动WalkBG服务**
   \`\`\`bash
   mvn spring-boot:run
   \`\`\`

2. **验证服务状态**
   \`\`\`bash
   curl http://localhost:8080/actuator/health
   \`\`\`

### **执行脚本**

1. **使用完整版脚本**
   \`\`\`bash
   ./create_route.sh
   \`\`\`

2. **使用简化版脚本**
   \`\`\`bash
   ./create_simple_route.sh
   \`\`\`

### **验证结果**

1. **查看所有路线**
   \`\`\`bash
   curl "http://localhost:8080/api/routes?page=0&size=10"
   \`\`\`

2. **查看特定路线详情**
   \`\`\`bash
   curl "http://localhost:8080/api/routes/{route_id}"
   \`\`\`

3. **查看路线详细信息（包含关联数据）**
   \`\`\`bash
   curl "http://localhost:8080/api/routes/{route_id}/details"
   \`\`\`

## 🔧 **自定义脚本**

### **修改服务地址**
\`\`\`bash
# 在脚本中修改
BASE_URL="http://your-server:port"
\`\`\`

### **自定义路线数据**

可以修改脚本中的JSON数据来创建不同的路线：

\`\`\`json
{
  "name": "你的路线名称",
  "description": "路线描述",
  "region": "地区",
  "distance": 10.0,
  "duration": 6,
  "difficulty": 2,
  "created_by": "your_user_id",
  "waypoints": [...],
  "segments": [...],
  "tags": [...],
  "seasons": [...]
}
\`\`\`

## 📊 **数据验证规则**

### **必填字段**
- `name` - 路线名称不能为空
- `created_by` - 创建者ID不能为空

### **数值范围**
- `difficulty`: 0-3 (0:简单, 1:中等, 2:困难, 3:极难)
- `route_type`: 0-3 (0:往返, 1:环线, 2:单程, 3:多日)
- `route_direction`: 0-2 (0:顺时针, 1:逆时针, 2:双向)
- `status`: 0-2 (0:规划中, 1:已发布, 2:已关闭)

### **数据精度**
- `distance`, `elevation_gain`, `elevation_loss`: BigDecimal (precision=8, scale=2)
- `latitude`, `longitude`: BigDecimal (precision=10, scale=6)

## 🐛 **故障排除**

### **常见错误**

1. **服务未启动**
   \`\`\`
   ❌ 服务未运行，请先启动 WalkBG 服务
   \`\`\`
   **解决方案**: 运行 `mvn spring-boot:run`

2. **数据验证失败 (HTTP 400)**
   \`\`\`
   ❌ 请求数据有误
   \`\`\`
   **解决方案**: 检查必填字段和数据格式

3. **服务器错误 (HTTP 500)**
   \`\`\`
   ❌ 服务器内部错误
   \`\`\`
   **解决方案**: 检查服务日志，确认数据库连接

### **调试技巧**

1. **查看详细请求信息**
   \`\`\`bash
   # 在curl命令中添加 -v 参数
   curl -v -X POST ...
   \`\`\`

2. **验证JSON格式**
   \`\`\`bash
   echo "$ROUTE_DATA" | python3 -m json.tool
   \`\`\`

3. **检查服务日志**
   \`\`\`bash
   # 查看Spring Boot应用日志
   tail -f logs/application.log
   \`\`\`

## 📚 **API文档**

访问Swagger文档获取完整的API信息：
\`\`\`
http://localhost:8080/swagger-ui.html
\`\`\`

## 🎯 **扩展功能**

脚本支持的扩展功能：
- 批量创建多条路线
- 从JSON文件读取路线数据
- 添加图片上传功能
- 集成地图数据验证

---

**版本**: v1.0  
**更新时间**: 2024-12-19  
**维护者**: WalkBG开发团队