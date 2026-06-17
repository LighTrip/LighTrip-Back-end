# .env의 시크릿을 Secrets Manager에 저장
# apply 후 AWS 콘솔에서 CHANGE_ME를 실제 값으로 교체
resource "aws_secretsmanager_secret" "app_secrets" {
  name = "lightrip/${var.environment}/app-secrets"

  tags = { Name = "lightrip-${var.environment}-secrets" }
}

resource "aws_secretsmanager_secret_version" "app_secrets" {
  secret_id = aws_secretsmanager_secret.app_secrets.id

  secret_string = jsonencode({
    DB_PASSWORD         = "CHANGE_ME"
    JWT_SECRET          = "CHANGE_ME"
    KAKAO_REST_API_KEY  = "CHANGE_ME"
    KAKAO_CLIENT_SECRET = "CHANGE_ME"
    TOSS_SECRET_KEY     = "CHANGE_ME"
    AI_SERVER_API_KEY   = "CHANGE_ME"
    SENTRY_DSN          = "CHANGE_ME"
    SENTRY_AUTH_TOKEN   = "CHANGE_ME"
  })

  # 콘솔에서 실제 값으로 교체한 뒤 terraform apply가 덮어쓰지 않도록
  lifecycle {
    ignore_changes = [secret_string]
  }
}
