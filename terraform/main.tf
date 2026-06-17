terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # 마이그레이션 시점에 S3 버킷 생성 후 주석 해제
  # State를 S3에 저장해야 팀원 간 공유 + 동시 수정 방지 가능
  # backend "s3" {
  #   bucket         = "lightrip-terraform-state"
  #   key            = "ecs/terraform.tfstate"
  #   region         = "ap-northeast-2"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region
}
