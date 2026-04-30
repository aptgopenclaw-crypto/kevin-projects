# Kevin Projects

此倉庫收錄多個專案，包含影像串流播放器與智慧路燈管理平台。

## 專案列表

| 專案 | 說明 |
|------|------|
| [arisan-video-player-v2](./arisan-video-player-v2/) | Vue 影像串流播放器元件，支援即時串流與回放功能 |
| [taipei-streetlight](./taipei-streetlight/) | 臺北市路燈管理平台 — 路燈資產全生命週期管理系統 |

## arisan-video-player-v2

Vue ESM 影像播放器，支援：

- 即時串流 (Live Streaming)
- 影像回放 (Playback)
- 時間軸搜尋 (Seekbar)

詳細整合說明請參閱 [arisan-video-player-v2/README.md](./arisan-video-player-v2/README.md)。

## taipei-streetlight

臺北市路燈管理平台，涵蓋設備資產、報修派工、材料管理、簽核流程等模組。

**技術棧：**

- **後端：** Java 21 · Spring Boot 3.4 · Spring Security · JPA/Hibernate · Flyway
- **前端：** Vue 3 · TypeScript · Element Plus · Pinia · ECharts
- **資料庫：** PostgreSQL 15+
- **認證：** JWT + CAPTCHA · RBAC 多租戶

**文件結構：** 需求規格 → 系統分析 → 系統設計 → 測試規格，完整 SDLC 文件。

詳細說明請參閱 [taipei-streetlight/README.md](./taipei-streetlight/README.md)。

## License

Private — All rights reserved.
