# 统一响应格式使用指南

## 🎯 **已实现的功能**

### ✅ **1. 统一的API响应格式**
\`\`\`kotlin
data class ApiResponse<T>(
    val success: Boolean = true,
    val message: String = "操作成功",
    val data: T? = null,
    val timestamp: Instant = Instant.now(),
    val code: Int = 200
)
\`\`\`

### ✅ **2. 全局异常处理**
- 自动处理所有异常并返回统一格式
- 支持参数验证异常、业务异常等
- 自动记录日志

### ✅ **3. 业务异常类**
\`\`\`kotlin
// 使用示例
throw BusinessException.badRequest("参数错误")
throw BusinessException.notFound("资源未找到")
throw BusinessException.forbidden("权限不足")
\`\`\`

### ✅ **4. 响应工具类**
\`\`\`kotlin
// 成功响应
ResponseUtil.success(data, "操作成功")

// 分页响应
ResponseUtil.successPage(pageData, "查询成功")

// 条件响应
ResponseUtil.conditional(data, "成功", "未找到")
\`\`\`

### ✅ **5. 基础查询请求**
\`\`\`kotlin
class BaseQueryRequest(
    val page: Int = 0,
    val size: Int = 10,
    val sortBy: String = "createdAt",
    val sortDir: String = "desc"
)
\`\`\`

## 🚀 **使用方法**

### **Controller层改造**

#### **改造前**：
\`\`\`kotlin
@GetMapping
fun getUsers(): ResponseEntity<Page<User>> {
    val users = userService.getAllUsers(pageable)
    return ResponseEntity.ok(users)
}

@GetMapping("/{id}")
fun getUserById(@PathVariable id: String): ResponseEntity<User> {
    val user = userService.getUserById(id)
    return if (user != null) {
        ResponseEntity.ok(user)
    } else {
        ResponseEntity.notFound().build()
    }
}
\`\`\`

#### **改造后**：
\`\`\`kotlin
@GetMapping
fun getUsers(
    @Valid @ModelAttribute request: BaseQueryRequest
): ResponseEntity<ApiResponse<Page<User>>> {
    val users = userService.getAllUsers(request.toPageable())
    return ResponseUtil.successPage(users, "获取用户列表成功")
}

@GetMapping("/{id}")
fun getUserById(
    @PathVariable id: String
): ResponseEntity<ApiResponse<User>> {
    val user = userService.getUserById(id)
    return ResponseUtil.conditional(
        data = user,
        successMessage = "获取用户成功",
        notFoundMessage = "用户不存在"
    )
}
\`\`\`

### **异常处理**

#### **改造前**：
\`\`\`kotlin
@PostMapping
fun createUser(@RequestBody user: User): ResponseEntity<User> {
    if (user.name.isBlank()) {
        return ResponseEntity.badRequest().build()
    }
    val created = userService.createUser(user)
    return ResponseEntity.status(HttpStatus.CREATED).body(created)
}
\`\`\`

#### **改造后**：
\`\`\`kotlin
@PostMapping
fun createUser(
    @Valid @RequestBody user: User
): ResponseEntity<ApiResponse<User>> {
    // 业务验证 - 抛出异常会被全局处理器捕获
    if (user.name.isBlank()) {
        throw BusinessException.badRequest("用户名不能为空")
    }
    
    val created = userService.createUser(user)
    return ResponseUtil.created(created, "创建用户成功")
}
\`\`\`

## 📊 **API响应示例**

### **成功响应**：
\`\`\`json
{
  "success": true,
  "message": "获取用户列表成功",
  "data": {
    "content": [...],
    "pageable": {...},
    "totalElements": 100
  },
  "timestamp": "2024-12-19T10:30:00Z",
  "code": 200
}
\`\`\`

### **错误响应**：
\`\`\`json
{
  "success": false,
  "message": "用户名不能为空",
  "data": null,
  "timestamp": "2024-12-19T10:30:00Z",
  "code": 400
}
\`\`\`

### **参数验证错误**：
\`\`\`json
{
  "success": false,
  "message": "参数验证失败: name: 不能为空, email: 邮箱格式不正确",
  "data": null,
  "timestamp": "2024-12-19T10:30:00Z",
  "code": 400
}
\`\`\`

## 🔧 **下一步改造建议**

### **1. 立即改造的Controller**
- ✅ WaterSourceController（已完成）
- 🔄 RouteController
- 🔄 UserController
- 🔄 SupplyController

### **2. 改造步骤**
1. 导入必要的类
2. 修改方法返回类型
3. 使用ResponseUtil工具类
4. 添加参数验证
5. 使用BusinessException抛出业务异常

### **3. 改造模板**
\`\`\`kotlin
// 导入
import org.example.common.dto.ApiResponse
import org.example.common.dto.BaseQueryRequest
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import jakarta.validation.Valid

// Controller类添加@Validated注解
@Validated
class YourController {
    
    // 列表查询
    @GetMapping
    fun getList(
        @Valid @ModelAttribute request: BaseQueryRequest
    ): ResponseEntity<ApiResponse<Page<Entity>>> {
        val data = service.getAll(request.toPageable())
        return ResponseUtil.successPage(data, "查询成功")
    }
    
    // 单个查询
    @GetMapping("/{id}")
    fun getById(@PathVariable id: String): ResponseEntity<ApiResponse<Entity>> {
        val data = service.getById(id)
        return ResponseUtil.conditional(data, "查询成功", "资源不存在")
    }
    
    // 创建
    @PostMapping
    fun create(@Valid @RequestBody entity: Entity): ResponseEntity<ApiResponse<Entity>> {
        if (/* 业务验证 */) {
            throw BusinessException.badRequest("验证失败")
        }
        val created = service.create(entity)
        return ResponseUtil.created(created, "创建成功")
    }
    
    // 更新
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody entity: Entity
    ): ResponseEntity<ApiResponse<Entity>> {
        val updated = service.update(id, entity)
        return ResponseUtil.conditional(updated, "更新成功", "资源不存在")
    }
    
    // 删除
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<ApiResponse<Nothing>> {
        val deleted = service.delete(id)
        return ResponseUtil.conditionalOperation(deleted, "删除成功", "删除失败")
    }
}
\`\`\`

## 🎉 **改造效果**

### **优势**：
1. ✅ **统一的响应格式** - 前端处理更简单
2. ✅ **自动异常处理** - 减少重复代码
3. ✅ **参数自动验证** - 提高代码质量
4. ✅ **统一的分页处理** - 简化分页逻辑
5. ✅ **更好的错误信息** - 提升用户体验

### **代码减少**：
- 每个Controller方法减少5-10行代码
- 消除重复的异常处理逻辑
- 统一的分页参数处理

你想先改造哪个Controller？我可以帮你逐个改造。