# Thần Khí Video Editor V5

Android video editor mobile-first, tiếp tục từ V4.

## V5 đã hoàn thiện thêm
- Render toàn bộ danh sách clip.
- WorkManager chờ Transformer hoàn tất trước khi đánh dấu job thành công.
- Transformer dùng Composition + EditedMediaItemSequence.
- Trim bằng MediaItem ClippingConfiguration.
- Hàng đợi render nền, retry/backoff và progress.
- ChunkPlanner cho pipeline video dài.
- SRT parser, subtitle model và Android TTS preview.
- Kiến trúc AI adapter để nối ASR / dịch / TTS thật mà không nhúng secret vào APK.

## Build APK
GitHub Actions tự động build `assembleDebug` khi push lên `main` hoặc chạy thủ công.

## AI production pipeline
`Video -> Audio/ASR -> timestamp -> SRT -> Translation -> TTS -> Composition/Audio -> Render MP4`

Các provider AI cần được cấu hình riêng; không hard-code API key trong source/APK.
