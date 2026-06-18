# ALB 보안 그룹 — 외부에서 80/443만 허용
resource "aws_security_group" "alb" {
  name        = "lightrip-${var.environment}-alb-sg"
  description = "ALB inbound HTTP/HTTPS"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "lightrip-${var.environment}-alb-sg" }
}

# ECS Task 보안 그룹 — ALB에서만 앱 포트 접근 허용
resource "aws_security_group" "ecs_task" {
  name        = "lightrip-${var.environment}-ecs-sg"
  description = "ECS Task - ALB only"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = var.app_port
    to_port         = var.app_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "lightrip-${var.environment}-ecs-sg" }
}

# ElastiCache 보안 그룹 — ECS Task에서만 6379 접근 허용
resource "aws_security_group" "elasticache" {
  name        = "lightrip-${var.environment}-redis-sg"
  description = "ElastiCache - ECS Task only"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_task.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "lightrip-${var.environment}-redis-sg" }
}

# 기존 RDS 보안 그룹에 ECS Task 접근 허용 규칙 추가
# 기존 SG를 "관리"하지 않고 규칙만 추가 — 다른 인바운드 규칙에 영향 없음
resource "aws_security_group_rule" "rds_from_ecs" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  security_group_id        = var.rds_security_group_id
  source_security_group_id = aws_security_group.ecs_task.id
  description              = "ECS Task to RDS"
}
