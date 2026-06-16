-- Passport 테이블에 pgvector 임베딩 컬럼 추가 (Issue #118)
-- > embedding: EmbeddingGemma가 생성한 1536차원 벡터. content(여행 기록 텍스트)를 임베딩한 값.
-- > RAG 초안 생성 시 유사한 과거 기록을 semantic search로 찾는 데 활용.
-- > nullable: 여권 저장 후 비동기로 채워지므로 초기값 NULL 허용.
ALTER TABLE public.passport ADD COLUMN IF NOT EXISTS embedding vector(1536);

-- IVFFlat 인덱스 (코사인 유사도 기준)
-- > HNSW 대신 IVFFlat 선택 이유: RDS db.t4g.micro 1GiB RAM 제약 — HNSW는 OOM 위험.
-- > lists=20: 데모 규모(수백~수천 건) 기준 적정값. 20개 클러스터로 벡터 공간 분할.
-- > vector_cosine_ops: 코사인 유사도 기반 검색 (의미적 유사성에 적합).
CREATE INDEX IF NOT EXISTS idx_passport_embedding_ivfflat
    ON public.passport
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 20);
