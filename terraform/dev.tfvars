environment = "dev"

# TODO: prod.tfvars와 동일한 서브넷 사용 가능
subnet_ids = [
  "subnet-xxxxxxxxxxxxxxxxx",
  "subnet-yyyyyyyyyyyyyyyyy",
]

task_cpu      = 256 # 0.25 vCPU
task_memory   = 512 # 0.5GB
desired_count = 1
app_port      = 8080
