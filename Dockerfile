# walkbg 后端镜像
#
# 多阶段构建：第一阶段编译，第二阶段只保留运行时所需的 JRE 与 jar，
# 避免把 Maven、源码和 ~/.m2 缓存带进最终镜像。

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Maven 镜像配置：国内直连 Maven Central 极慢（实测数十 KB/s），
# 改用同厂镜像后延迟约 0.1s。
COPY maven-settings.xml /usr/share/maven/conf/settings-mirror.xml

# 先只复制 pom.xml 并预下载依赖：
# 只要 pom.xml 没变，这一层就能命中缓存，后续改代码无需重新拉取依赖。
COPY pom.xml .
RUN mvn -B -s /usr/share/maven/conf/settings-mirror.xml dependency:go-offline

COPY src ./src

# 跳过测试：镜像构建不是运行测试的地方，测试应在构建镜像之前独立执行。
RUN mvn -B -s /usr/share/maven/conf/settings-mirror.xml clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

# 以非 root 用户运行：容器内进程一旦被利用，降低影响范围。
RUN groupadd --system --gid 1001 appuser \
    && useradd --system --uid 1001 --gid appuser appuser

# 通配符匹配版本号变化，避免版本升级后 Dockerfile 失效
COPY --from=builder /build/target/walkbg-*.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

# JVM 参数说明：
# - MaxRAMPercentage 让堆大小随容器内存限制自动伸缩，避免写死 -Xmx
#   在换机器后不匹配；容器环境下必须依赖容器感知而非物理内存。
# - UseSerialGC 在单核小内存机器上比 G1 开销更小。
# - 时区显式设为上海，避免容器默认 UTC 导致日志与业务时间错位。
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Duser.timezone=Asia/Shanghai"

# 用 exec 形式启动，使 java 成为 PID 1 直接接收 SIGTERM，
# 配合 server.shutdown=graceful 完成优雅停机。
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
