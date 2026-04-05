<?php
require_once 'config.php';

header('Content-Type: application/json');

$conn = getConnection();
$sessionId = $_SESSION['session_id'];
$userId = $_SESSION['user_id'] ?? null;

// Get cart items
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    if ($userId) {
        $stmt = $conn->prepare("
            SELECT c.*, p.name as product_name, p.price as unit_price 
            FROM cart c 
            JOIN products p ON c.product_id = p.id 
            WHERE c.user_id = ?
        ");
        $stmt->bind_param("i", $userId);
    } else {
        $stmt = $conn->prepare("
            SELECT c.*, p.name as product_name, p.price as unit_price 
            FROM cart c 
            JOIN products p ON c.product_id = p.id 
            WHERE c.session_id = ?
        ");
        $stmt->bind_param("s", $sessionId);
    }
    
    $stmt->execute();
    $result = $stmt->get_result();
    
    $cartItems = [];
    while ($row = $result->fetch_assoc()) {
        $cartItems[] = $row;
    }
    
    echo json_encode(['success' => true, 'cart' => $cartItems]);
    $stmt->close();
    $conn->close();
    exit;
}

// Add to cart
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'), true);
    
    $productId = $data['productId'] ?? 0;
    $productName = $data['productName'] ?? '';
    
    if ($productId <= 0) {
        echo json_encode(['success' => false, 'message' => 'Invalid product']);
        exit;
    }
    
    // Get product price
    $priceStmt = $conn->prepare("SELECT price FROM products WHERE id = ?");
    $priceStmt->bind_param("i", $productId);
    $priceStmt->execute();
    $priceResult = $priceStmt->get_result();
    
    if ($priceResult->num_rows === 0) {
        echo json_encode(['success' => false, 'message' => 'Product not found']);
        $priceStmt->close();
        $conn->close();
        exit;
    }
    
    $product = $priceResult->fetch_assoc();
    $price = $product['price'];
    $priceStmt->close();
    
    // Check if item already in cart
    if ($userId) {
        $checkStmt = $conn->prepare("SELECT id, quantity FROM cart WHERE user_id = ? AND product_id = ?");
        $checkStmt->bind_param("ii", $userId, $productId);
    } else {
        $checkStmt = $conn->prepare("SELECT id, quantity FROM cart WHERE session_id = ? AND product_id = ?");
        $checkStmt->bind_param("si", $sessionId, $productId);
    }
    
    $checkStmt->execute();
    $checkResult = $checkStmt->get_result();
    
    if ($checkResult->num_rows > 0) {
        // Update quantity
        $existing = $checkResult->fetch_assoc();
        $newQuantity = $existing['quantity'] + 1;
        $updateStmt = $conn->prepare("UPDATE cart SET quantity = ? WHERE id = ?");
        $updateStmt->bind_param("ii", $newQuantity, $existing['id']);
        $updateStmt->execute();
        $updateStmt->close();
    } else {
        // Insert new item
        $insertStmt = $conn->prepare("INSERT INTO cart (user_id, session_id, product_id, product_name, quantity, price) VALUES (?, ?, ?, ?, 1, ?)");
        $insertStmt->bind_param("isisd", $userId, $sessionId, $productId, $productName, $price);
        $insertStmt->execute();
        $insertStmt->close();
    }
    
    $checkStmt->close();
    
    // Log activity
    $userName = $_SESSION['user_name'] ?? 'Guest';
    $activityType = 'add_to_cart';
    $description = "Added to cart: $productName";
    $ip = getUserIP();
    
    $activityStmt = $conn->prepare("INSERT INTO activities (user_id, user_name, activity_type, description, ip_address) VALUES (?, ?, ?, ?, ?)");
    $activityStmt->bind_param("issss", $userId, $userName, $activityType, $description, $ip);
    $activityStmt->execute();
    $activityStmt->close();
    
    echo json_encode(['success' => true, 'message' => 'Item added to cart']);
    $conn->close();
    exit;
}

// Update cart item
if ($_SERVER['REQUEST_METHOD'] === 'PUT') {
    $data = json_decode(file_get_contents('php://input'), true);
    
    $cartId = $data['cartId'] ?? 0;
    $quantity = $data['quantity'] ?? 1;
    
    if ($quantity <= 0) {
        // Remove item
        $deleteStmt = $conn->prepare("DELETE FROM cart WHERE id = ?");
        $deleteStmt->bind_param("i", $cartId);
        $deleteStmt->execute();
        $deleteStmt->close();
    } else {
        // Update quantity
        $updateStmt = $conn->prepare("UPDATE cart SET quantity = ? WHERE id = ?");
        $updateStmt->bind_param("ii", $quantity, $cartId);
        $updateStmt->execute();
        $updateStmt->close();
    }
    
    echo json_encode(['success' => true, 'message' => 'Cart updated']);
    $conn->close();
    exit;
}

// Clear cart
if ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
    if ($userId) {
        $stmt = $conn->prepare("DELETE FROM cart WHERE user_id = ?");
        $stmt->bind_param("i", $userId);
    } else {
        $stmt = $conn->prepare("DELETE FROM cart WHERE session_id = ?");
        $stmt->bind_param("s", $sessionId);
    }
    
    $stmt->execute();
    $stmt->close();
    
    echo json_encode(['success' => true, 'message' => 'Cart cleared']);
    $conn->close();
    exit;
}

echo json_encode(['success' => false, 'message' => 'Invalid request']);
$conn->close();
?>