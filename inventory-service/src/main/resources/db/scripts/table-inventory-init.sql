CREATE DATABASE IF NOT EXISTS inventoryservice;

GRANT ALL PRIVILEGES ON inventoryservice.* TO 'ecommerce'@'%';

FLUSH PRIVILEGES;
INSERT INTO inventory (id, productName, quantity)
VALUES
    (1, 'Iphone 13', 12),
    (2, 'Macbook Pro 13.3', 4);
