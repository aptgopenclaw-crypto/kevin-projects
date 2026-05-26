在專案根目錄（或 backend 目錄）執行：

```bash
cd backend
mvn -Pprod -DskipTests clean package
```

產物：`backend/target/taipei-streetlight-0.0.1-SNAPSHOT.jar`（單一可執行 jar，內含前端靜態資源）。

執行 jar：

```bash
java -jar backend/target/taipei-streetlight-0.0.1-SNAPSHOT.jar
```

---



