CREATE TABLE trade_risk_result (
                                   id BIGSERIAL PRIMARY KEY,
                                   trade_id VARCHAR(50) NOT NULL,
                                   instrument VARCHAR(30) NOT NULL,
                                   currency VARCHAR(3) NOT NULL,
                                   quantity NUMERIC(19, 4) NOT NULL,
                                   price NUMERIC(19, 4) NOT NULL,
                                   exposure NUMERIC(24, 4) NOT NULL,
                                   business_date DATE NOT NULL,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT uk_trade_risk_result_trade_date
                                       UNIQUE (trade_id, business_date),

                                   CONSTRAINT chk_trade_risk_result_quantity
                                       CHECK (quantity > 0),

                                   CONSTRAINT chk_trade_risk_result_price
                                       CHECK (price >= 0)
);

CREATE INDEX idx_trade_risk_result_business_date
    ON trade_risk_result (business_date);

CREATE INDEX idx_trade_risk_result_currency
    ON trade_risk_result (currency);


CREATE TABLE trade_risk_rejection (
                                      id BIGSERIAL PRIMARY KEY,
                                      job_execution_id BIGINT,
                                      input_trade_id VARCHAR(50),
                                      rejection_reason VARCHAR(500) NOT NULL,
                                      raw_record TEXT,
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trade_risk_rejection_job_execution
    ON trade_risk_rejection (job_execution_id);