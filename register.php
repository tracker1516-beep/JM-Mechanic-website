<?php
require_once 'config.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'), true);
    
    $fullName = $data['fullName'] ?? '';
    $email = $data['email'] ?? '';
    $phone = $data['phone'] ?? '';
    $password = $data['password'] ?? '';
    $likeProducts = $data['likeProducts'] ?? true;
    
    // Validate input
    if (empty($fullName) || empty($email) || empty($phone) || empty($password)) {
        echo json_encode(['success' => false, 'message' => 'All fields are required']);
        exit;
    }
    
    $conn = getConnection();
    
    // Check if user already exists
    $stmt = $conn->prepare("SELECT id FROM users WHERE email = ?");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows > 0) {
        echo json_encode(['success' => false, 'message' => 'Email already registered']);
        $stmt->close();
        $conn->close();
        exit;
    }
    
    // Hash password
    $hashedPassword = password_hash($password, PASSWORD_DEFAULT);
    
    // Insert user
    $stmt = $conn->prepare("INSERT INTO users (full_name, email, phone, password, likes_products) VALUES (?, ?, ?, ?, ?)");
    $stmt->bind_param("ssssi", $fullName, $email, $phone, $hashedPassword, $likeProducts);
    
    if ($stmt->execute()) {
        $userId = $stmt->insert_id;
        
        // Log activity
        $activityType = 'registration';
        $description = "New user registered: $fullName";
        $ip = getUserIP();
        
        $activityStmt = $conn->prepare("INSERT INTO activities (user_id, user_name, activity_type, description, ip_address) VALUES (?, ?, ?, ?, ?)");
        $activityStmt->bind_param("issss", $userId, $fullName, $activityType, $description, $ip);
        $activityStmt->execute();
        $activityStmt->close();
        
        // Return user data (without password)
        $userData = [
            'id' => $userId,
            'fullName' => $fullName,
            'email' => $email,
            'phone' => $phone,
            'registrationDate' => date('Y-m-d H:i:s'),
            'likeProducts' => (bool)$likeProducts
        ];
        
        echo json_encode(['success' => true, 'message' => 'Registration successful', 'user' => $userData]);
    } else {
        echo json_encode(['success' => false, 'message' => 'Registration failed: ' . $conn->error]);
    }
    
    $stmt->close();
    $conn->close();
} else {
    echo json_encode(['success' => false, 'message' => 'Invalid request method']);
}
?>