resource "aws_ecr_repository" "api" {
  name                 = "lightrip-api-${var.environment}"
  # > IMMUTABLE: 동일 태그로 덮어쓰기 불가 — CI/CD에서 :latest 대신 커밋 SHA 태그 사용 필요
  image_tag_mutability = "IMMUTABLE"
  force_delete         = false

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = { Name = "lightrip-${var.environment}-ecr" }
}

# 최근 10개만 보관, 나머지 자동 삭제 — ECR 스토리지 비용 절감
resource "aws_ecr_lifecycle_policy" "api" {
  repository = aws_ecr_repository.api.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}
