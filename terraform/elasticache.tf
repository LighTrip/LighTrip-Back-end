# EC2에 직접 설치된 Redis(현재 10.0.3.247:6379)를 대체 — JWT 블랙리스트 + Refresh Token + 실시간 위치 저장
# ECS Task SG에서만 6379 접근 허용 (sg.tf의 elasticache SG 참조)
resource "aws_elasticache_subnet_group" "redis" {
  name       = "lightrip-${var.environment}-redis"
  subnet_ids = var.subnet_ids

  tags = { Name = "lightrip-${var.environment}-redis-subnet" }
}

resource "aws_elasticache_cluster" "redis" {
  cluster_id           = "lightrip-${var.environment}"
  engine               = "redis"
  engine_version       = "7.1"
  node_type            = var.elasticache_node_type
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.redis.name
  security_group_ids   = [aws_security_group.elasticache.id]

  tags = { Name = "lightrip-${var.environment}-redis" }
}
