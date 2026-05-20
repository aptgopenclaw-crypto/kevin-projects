# 先確認 Playwright browser 已安裝（只需一次）
cd fix-data
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"

# 方式一：Maven 執行（開發時使用）
mvn spring-boot:run -Dspring-boot.run.arguments="--from=2026-01-01 --to=2026-05-13"

# 方式二：fat jar 執行（推薦）
java -jar target/fix-data-1.0.0.jar --from=2026-01-01 --to=2026-05-13