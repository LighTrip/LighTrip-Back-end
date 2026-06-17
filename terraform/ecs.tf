resource "aws_ecs_cluster" "main" {
  name = "lightrip-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = { Name = "lightrip-${var.environment}-cluster" }
}

resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/lightrip-${var.environment}"
  retention_in_days = 30

  tags = { Name = "lightrip-${var.environment}-logs" }
}

resource "aws_ecs_task_definition" "api" {
  family                   = "lightrip-${var.environment}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "lightrip-api"
    image = "${aws_ecr_repository.api.repository_url}:latest"

    portMappings = [{
      containerPort = var.app_port
      protocol      = "tcp"
    }]

    environment = [
      { name = "DB_HOST", value = var.db_host },
      { name = "DB_NAME", value = "lightrip" },
      { name = "DB_USERNAME", value = "lightrip" },
      { name = "DDL_AUTO", value = "none" },
      { name = "SHOW_SQL", value = "false" },
      { name = "FLYWAY_ENABLED", value = "true" },
      { name = "DEEP_LINK_SCHEME", value = "lightrip" },
      { name = "REDIS_HOST", value = aws_elasticache_cluster.redis.cache_nodes[0].address },
      { name = "AWS_REGION", value = var.aws_region },
      { name = "AWS_S3_BUCKET", value = "lightrip-s3" },
      { name = "CLOUDFRONT_DOMAIN", value = "cdn.lightrip.cloud" },
      { name = "SPRING_PROFILES_ACTIVE", value = var.environment },
    ]

    secrets = [
      { name = "DB_PASSWORD", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:DB_PASSWORD::" },
      { name = "JWT_SECRET", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:JWT_SECRET::" },
      { name = "KAKAO_REST_API_KEY", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:KAKAO_REST_API_KEY::" },
      { name = "KAKAO_CLIENT_SECRET", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:KAKAO_CLIENT_SECRET::" },
      { name = "TOSS_SECRET_KEY", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:TOSS_SECRET_KEY::" },
      { name = "AI_SERVER_API_KEY", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:AI_SERVER_API_KEY::" },
      { name = "SENTRY_DSN", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:SENTRY_DSN::" },
      { name = "SENTRY_AUTH_TOKEN", valueFrom = "${aws_secretsmanager_secret.app_secrets.arn}:SENTRY_AUTH_TOKEN::" },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])

  tags = { Name = "lightrip-${var.environment}-task" }
}

resource "aws_ecs_service" "api" {
  name            = "lightrip-${var.environment}-api"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.subnet_ids
    security_groups  = [aws_security_group.ecs_task.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = "lightrip-api"
    container_port   = var.app_port
  }

  # 배포 실패 시 자동 롤백
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  # GitHub Actions가 update-service로 새 이미지 배포 — Terraform이 되돌리지 않도록
  lifecycle {
    ignore_changes = [task_definition]
  }

  depends_on = [aws_lb_listener.https]

  tags = { Name = "lightrip-${var.environment}-service" }
}
