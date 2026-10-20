# 資安說明

## 絕對不要提交的檔案

- 上傳金鑰或正式簽署金鑰（`.jks`、`.keystore`、`.p12`）與密碼。
- `keystore.properties`、`local.properties`。
- Google Play 服務帳戶 JSON、`google-services.json`、任何 API Key。
- GitHub 個人存取權杖或其他憑證。
- 匯出的掃描結果、Logs，或不屬於你的網路截圖。

`keystore.properties.example` 只是範本。真正的金鑰檔請放在專案資料夾外面。

## 每次 Push 前

```bash
python scripts/security_scan.py .
```

GitHub Actions 會在每次 Push 和 Pull Request 執行同一支掃描程式。

## 不小心外洩時

1. 立刻撤銷或更換。上傳金鑰外洩時，到 Play Console 申請重設上傳金鑰。
2. 從目前的檔案中移除。
3. 如果已經提交，要改寫 Git 歷史；只刪掉最新版的檔案不夠。
4. 重新執行掃描後再 Push。

## App 隱私

App 沒有網路功能，不收集任何資料，掃描結果只存在手機記憶體。詳見[隱私權政策](PRIVACY.md)。

## 回報問題

請私下聯絡 Repo 擁有者，不要在 Issue 公開仍有效的憑證。
