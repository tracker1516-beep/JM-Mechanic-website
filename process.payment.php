<?php
require_once 'config.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'), true);
    
    $paymentMethod = $data['paymentMethod'] ?? '';
    $cardNumber = $data['cardNumber'] ?? '';
    $expiryDate = $data['expiryDate'] ?? '';
    $cvv = $data['cvv'] ?? '';
    $cardholderName = $data['cardholderName'] ?? '';
    $mobileNumber = $data['mobileNumber'] ?? '';
    $transactionRef = $data['transactionRef'] ?? '';
    
    $userId = $_SESSION['user_id'] ?? null;
    $userName = $_SESSION['user_name'] ?? 'Guest';
    $userEmail = $_SESSION['user_email'] ?? '';
    $userPhone = $_SESSION['user_phone'] ?? '';
    
    $conn = getConnection();
    
    // Start transaction
    $conn->begin_transaction();
    
    try {
        // Get cart items
        if ($userId) {
            $cartStmt = $conn->prepare("SELECT * FROM cart WHERE user_id = ?");
            $cartStmt->bind_param("i", $userId);
        } else {
            $sessionId = $_SESSION['session_id'];
            $cartStmt = $conn->prepare("SELECT * FROM cart WHERE session_id = ?");
            $cartStmt->bind_param("s", $sessionId);
        }
        
        $cartStmt->execute();
        $cartResult = $cartStmt->get_result();
        $cartItems = [];
        $totalAmount = 0;
        
        while ($item = $cartResult->fetch_assoc()) {
            $cartItems[] = $item;
            $totalAmount += $item['price'] * $item['quantity'];
        }
        $cartStmt->close();
        
        if (empty($cartItems)) {
            throw new Exception('Cart is empty');
        }
        
        // Generate order ID
        $orderId = 'ORD-' . date('Ymd') . '-' . strtoupper(uniqid());
        
        // Create order
        $orderStmt = $conn->prepare("INSERT INTO orders (order_id, user_id, customer_name, customer_email, customer_phone, total_amount, status) VALUES (?, ?, ?, ?, ?, ?, 'completed')");
        $orderStmt->bind_param("sisssd", $orderId, $userId, $userName, $userEmail, $userPhone, $totalAmount);
        $orderStmt->execute();
        $orderStmt->close();
        
        // Insert order items
        $itemStmt = $conn->prepare("INSERT INTO order_items (order_id, product_id, product_name, quantity, price) VALUES (?, ?, ?, ?, ?)");
        
        foreach ($cartItems as $item) {
            $itemStmt->bind_param("sisid", $orderId, $item['product_id'], $item['product_name'], $item['quantity'], $item['price']);
            $itemStmt->execute();
        }
        $itemStmt->close();
        
        // Generate transaction ID
        $transactionId = 'TXN-' . date('YmdHis') . '-' . strtoupper(uniqid());
        
        // Create payment record
        $paymentStmt = $conn->prepare("INSERT INTO payments (transaction_id, order_id, amount, payment_method, card_number, mobile_number, status, recipient_number) VALUES (?, ?, ?, ?, ?, ?, 'completed', '0786497932')");
        $paymentStmt->bind_param("ssdssss", $transactionId, $orderId, $totalAmount, $paymentMethod, $cardNumber, $mobileNumber);
        $paymentStmt->execute();
        $paymentStmt->close();
        
        // Clear cart
        if ($userId) {
            $clearStmt = $conn->prepare("DELETE FROM cart WHERE user_id = ?");
            $clearStmt->bind_param("i", $userId);
        } else {
            $sessionId = $_SESSION['session_id'];
            $clearStmt = $conn->prepare("DELETE FROM cart WHERE session_id = ?");
            $clearStmt->bind_param("s", $sessionId);
        }
        $clearStmt->execute();
        $clearStmt->close();
        
        // Log activity
        $activityType = 'payment';
        $description = "Payment completed: Order $orderId - $transactionId - $$totalAmount via $paymentMethod";
        $ip = getUserIP();
        
        $activityStmt = $conn->prepare("INSERT INTO activities (user_id, user_name, activity_type, description, ip_address) VALUES (?, ?, ?, ?, ?)");
        $activityStmt->bind_param("issss", $userId, $userName, $activityType, $description, $ip);
        $activityStmt->execute();
        $activityStmt->close();
        
        // Send email notification (simulated)
        $emailBody = "New Payment Received:\n\n";
        $emailBody .= "Order ID: $orderId\n";
        $emailBody .= "Transaction ID: $transactionId\n";
        $emailBody .= "Customer: $userName\n";
        $emailBody .= "Email: $userEmail\n";
        $emailBody .= "Phone: $userPhone\n";
        $emailBody .= "Amount: $$totalAmount\n";
        $emailBody .= "Payment Method: $paymentMethod\n";
        $emailBody .= "Payment to: 0786497932\n\n";
        $emailBody .= "Order Date: " . date('Y-m-d H:i:s') . "\n";
        
        // In production, use mail() or PHPMailer
        error_log("Payment email to jamesmulauzi47@gmail.com: " . $emailBody);
        
        $conn->commit();
        
        echo json_encode([
            'success' => true,
            'message' => 'Payment successful',
            'orderId' => $orderId,
            'transactionId' => $transactionId,
            'amount' => $totalAmount
        ]);
        
    } catch (Exception $e) {
        $conn->rollback();
        echo json_encode(['success' => false, 'message' => 'Payment failed: ' . $e->getMessage()]);
    }
    
    $conn->close();
} else {
    echo json_encode(['success' => false, 'message' => 'Invalid request method']);
}
?>