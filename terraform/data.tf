# 기존 VPC를 "참조"만 함 — Terraform이 관리(변경/삭제)하지 않음
data "aws_vpc" "main" {
  id = var.vpc_id
}

data "aws_caller_identity" "current" {}
