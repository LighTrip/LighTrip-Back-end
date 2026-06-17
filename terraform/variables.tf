variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "environment" {
  description = "배포 환경 (prod / dev)"
  type        = string
}

variable "vpc_id" {
  description = "기존 VPC ID"
  type        = string
  default     = "vpc-03baa8243e6af7bc4"
}

# ALB는 최소 2개 AZ의 서브넷이 필수
# VPC 콘솔 > 서브넷 > vpc-03baa8243e6af7bc4 필터 > 라우팅 테이블에 igw-xxx가 있는 서브넷이 퍼블릭
variable "subnet_ids" {
  description = "퍼블릭 서브넷 ID 목록 (최소 2개, 서로 다른 AZ)"
  type        = list(string)
}

variable "task_cpu" {
  description = "Fargate Task CPU (256 = 0.25 vCPU, 512 = 0.5 vCPU)"
  type        = number
}

variable "task_memory" {
  description = "Fargate Task 메모리 (MB)"
  type        = number
}

variable "app_port" {
  description = "Spring Boot 앱 포트"
  type        = number
  default     = 8080
}

variable "desired_count" {
  description = "ECS 서비스 Task 수"
  type        = number
  default     = 1
}

variable "db_host" {
  description = "RDS 엔드포인트"
  type        = string
  default     = "lightrip-db.cbg2c2egkvme.ap-northeast-2.rds.amazonaws.com"
}

variable "rds_security_group_id" {
  description = "기존 RDS 보안 그룹 ID — ECS 접근 허용 규칙 추가 대상"
  type        = string
  default     = "sg-0359d60e73c922ea2"
}

variable "acm_certificate_arn" {
  description = "ACM 인증서 ARN (*.lightrip.cloud)"
  type        = string
  default     = "arn:aws:acm:ap-northeast-2:423405486977:certificate/6d271c6f-9b07-40a6-a578-659ec48e1819"
}

variable "elasticache_node_type" {
  description = "ElastiCache 노드 타입"
  type        = string
  default     = "cache.t4g.micro"
}
