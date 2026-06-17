environment = "prod"

# TODO: VPC 콘솔 > 서브넷 > vpc-03baa8243e6af7bc4 필터
# 라우팅 테이블에 igw-xxx가 있는 서브넷 = 퍼블릭
# 최소 2개, 서로 다른 AZ 필수 (예: ap-northeast-2a, 2c)
subnet_ids = [
  "subnet-02dd370f201c22443",
  "subnet-0ccf2d37ff8ee3fcf",
]

task_cpu      = 512  # 0.5 vCPU
task_memory   = 1024 # 1GB
desired_count = 1
app_port      = 8080
