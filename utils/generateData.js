const { faker}  = require('@faker-js/faker');

const N = 100; // Number of customers
const M = 10_000; // Number of orders
const P = 200; // Number of products
const MAX_PRODUCTS_PER_ORDER = 3;

// Faker output can contain apostrophes (e.g. "O'Reilly"), which would otherwise break the
// generated SQL string literals.
function escapeSql(value) {
    return value.replace(/'/g, "''");
}

// Generate customers
for (let i = 1; i <= N; i++) {
    console.log(`INSERT INTO customer (id, name) VALUES (${i}, '${escapeSql(faker.name.fullName())}');`);
}

// Generate products
for (let i = 1; i <= P; i++) {
    console.log(`INSERT INTO product (id, description) VALUES (${i}, '${escapeSql(faker.commerce.productName())}');`);
}

// Generate orders
for (let i = 1; i <= M; i++) {
    const customerId = Math.ceil(Math.random() * N);
    console.log(`INSERT INTO "order" (id, description, customer_id) VALUES (${i}, '${escapeSql(faker.commerce.productName())}', ${customerId});`);
}

// Generate order_product associations - every order contains 1 or more products
for (let orderId = 1; orderId <= M; orderId++) {
    const productCount = Math.ceil(Math.random() * MAX_PRODUCTS_PER_ORDER);
    const productIds = new Set();
    while (productIds.size < productCount) {
        productIds.add(Math.ceil(Math.random() * P));
    }
    for (const productId of productIds) {
        console.log(`INSERT INTO order_product (order_id, product_id) VALUES (${orderId}, ${productId});`);
    }
}
