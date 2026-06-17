output "alb_dns_name" {
  description = "ALB DNS — Cloudflare에서 api.lightrip.cloud CNAME을 이 값으로 변경"
  value       = aws_lb.api.dns_name
}

output "ecr_repository_url" {
  description = "ECR URL — GitHub Actions에서 docker push 대상"
  value       = aws_ecr_repository.api.repository_url
}

output "ecs_cluster_name" {
  description = "ECS 클러스터 이름 — aws ecs update-service에 사용"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS 서비스 이름 — aws ecs update-service에 사용"
  value       = aws_ecs_service.api.name
}

output "redis_endpoint" {
  description = "ElastiCache Redis 엔드포인트"
  value       = aws_elasticache_cluster.redis.cache_nodes[0].address
}

output "secrets_manager_arn" {
  description = "Secrets Manager ARN — 시크릿 값 등록 시 참조"
  value       = aws_secretsmanager_secret.app_secrets.arn
}
