# TH5 TEST EVIDENCE ARTIFACTS

- `../postman/TH5_Security.postman_collection.json`: collection gốc 01–09, giữ nguyên để tái hiện demo/KT1 ban đầu.
- `../postman/TH5_Security_Extended_Evidence.postman_collection.json`: collection mở rộng dùng cho regression TH5; bổ sung Room Manager create 201, thời gian Reservation động và Student02 isolation.
- `test-results.md`: kết quả runtime đã chạy S01–S13. Audit receipt được kiểm tại Audit terminal sau Reservation 201.

Quy ước: chạy từng request theo thứ tự login → protected API/RBAC → ownership → machine identity/Audit; chỉ chạy Runner sau khi đã hiểu từng response. Không lưu full JWT hoặc secret trong report.
