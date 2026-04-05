<script>
// Database simulation (replaced with actual API calls)
let userData = null;

// Base URL for API calls
const API_BASE = 'api/'; // Create an api/ directory with the PHP files

// WhatsApp integration functions
function joinWhatsAppChannel() {
    window.open('https://chat.whatsapp.com/I0a9YRBvvUGHFOEUSvvUuJ', '_blank');
}

function joinWhatsAppGroup() {
    window.open('https://chat.whatsapp.com/I0a9YRBvvUGHFOEUSvvUuJ', '_blank');
}

// Close WhatsApp banner
document.getElementById('closeBanner').addEventListener('click', function() {
    document.getElementById('whatsappBanner').style.display = 'none';
});

// Page Navigation
function showPage(pageId) {
    // Hide all pages
    document.querySelectorAll('.page-section').forEach(page => {
        page.classList.remove('active');
    });
    
    // Show selected page
    document.getElementById(pageId).classList.add('active');
    
    // Scroll to top
    window.scrollTo(0, 0);
    
    // If payment page is shown, update the cart summary
    if (pageId === 'payment') {
        updatePaymentCartSummary();
    }
}

// Function to go to payment page from cart
function goToPayment() {
    // Close cart modal
    cartModal.style.display = 'none';
    // Show payment page
    showPage('payment');
}

// Update payment page cart summary
async function updatePaymentCartSummary() {
    const paymentCartItems = document.getElementById('payment-cart-items');
    const paymentTotal = document.getElementById('payment-total');
    const paymentTax = document.getElementById('payment-tax');
    
    try {
        const response = await fetch(`${API_BASE}cart.php`);
        const data = await response.json();
        
        if (!data.success || data.cart.length === 0) {
            paymentCartItems.innerHTML = '<div class="summary-item"><span>Your cart is empty</span><span>$0.00</span></div>';
            paymentTotal.textContent = '$0.00';
            paymentTax.textContent = '$0.00';
            return;
        }
        
        // Calculate subtotal
        let subtotal = 0;
        data.cart.forEach(item => {
            subtotal += item.price * item.quantity;
        });
        
        // Calculate tax (15%)
        const tax = subtotal * 0.15;
        const total = subtotal + tax;
        
        // Update cart items display
        paymentCartItems.innerHTML = '';
        data.cart.forEach(item => {
            const itemTotal = item.price * item.quantity;
            
            const itemElement = document.createElement('div');
            itemElement.className = 'summary-item';
            itemElement.innerHTML = `
                <span>${item.product_name} (x${item.quantity})</span>
                <span>$${itemTotal.toFixed(2)}</span>
            `;
            paymentCartItems.appendChild(itemElement);
        });
        
        // Update totals
        paymentTax.textContent = `$${tax.toFixed(2)}`;
        paymentTotal.textContent = `$${total.toFixed(2)}`;
        
        // Update payment success message amount
        document.getElementById('payment-amount').textContent = `$${total.toFixed(2)}`;
        
        // Generate order reference
        const orderId = `ORD-${Math.floor(10000 + Math.random() * 90000)}`;
        document.getElementById('order-ref').textContent = orderId;
        document.getElementById('paynow-ref').textContent = orderId;
        document.getElementById('success-order-id').textContent = orderId;
        
        // Generate transaction ID
        const transactionId = `TXN-${Math.floor(100000 + Math.random() * 900000)}`;
        document.getElementById('transaction-id').textContent = transactionId;
        
    } catch (error) {
        console.error('Error fetching cart:', error);
        paymentCartItems.innerHTML = '<div class="summary-item"><span>Error loading cart</span><span>--</span></div>';
        paymentTotal.textContent = '$0.00';
        paymentTax.textContent = '$0.00';
    }
}

// User Registration
document.getElementById('registrationForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    // Get form values
    const fullName = document.getElementById('fullName').value;
    const email = document.getElementById('email').value;
    const phone = document.getElementById('phone').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const likeProducts = document.getElementById('likeProducts').checked;
    
    // Validate passwords match
    if (password !== confirmPassword) {
        alert('Passwords do not match!');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}register.php`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                fullName,
                email,
                phone,
                password,
                likeProducts
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            // Store user data
            userData = data.user;
            
            // Update profile display
            updateProfileDisplay();
            
            // Show success message
            const successMessage = document.getElementById('successMessage');
            successMessage.style.display = 'block';
            
            // Reset form
            document.getElementById('registrationForm').reset();
            
            // Automatically like all products if user selected the option
            if (likeProducts) {
                document.querySelectorAll('.like-button').forEach(button => {
                    button.classList.add('liked');
                    button.innerHTML = '<i class="fas fa-heart"></i>';
                });
            }
            
            // Hide success message after 5 seconds
            setTimeout(() => {
                successMessage.style.display = 'none';
            }, 5000);
        } else {
            alert('Registration failed: ' + data.message);
        }
    } catch (error) {
        console.error('Registration error:', error);
        alert('Registration failed. Please try again.');
    }
});

// Update profile display
function updateProfileDisplay() {
    if (userData) {
        document.getElementById('profileName').textContent = userData.fullName;
        document.getElementById('profileEmail').textContent = userData.email;
        document.getElementById('profilePhone').textContent = userData.phone;
        document.getElementById('profileDate').textContent = userData.registrationDate;
        
        // Update edit form with current values
        document.getElementById('editFullName').value = userData.fullName;
        document.getElementById('editEmail').value = userData.email;
        document.getElementById('editPhone').value = userData.phone;
    }
}

// Cart functionality
let cart = [];
const cartIcon = document.getElementById('cartIcon');
const cartModal = document.getElementById('cartModal');
const closeCart = document.getElementById('closeCart');
const cartItems = document.getElementById('cartItems');
const cartCount = document.getElementById('cartCount');
const cartTotal = document.getElementById('cartTotal');
const checkoutBtn = document.getElementById('checkoutBtn');
const addToCartButtons = document.querySelectorAll('.add-to-cart');

// Like button functionality
const likeButtons = document.querySelectorAll('.like-button');
likeButtons.forEach(button => {
    button.addEventListener('click', async () => {
        button.classList.toggle('liked');
        if (button.classList.contains('liked')) {
            button.innerHTML = '<i class="fas fa-heart"></i>';
        } else {
            button.innerHTML = '<i class="far fa-heart"></i>';
        }
    });
});

// Open cart modal
cartIcon.addEventListener('click', async () => {
    cartModal.style.display = 'flex';
    await updateCartDisplay();
});

// Close cart modal
closeCart.addEventListener('click', () => {
    cartModal.style.display = 'none';
});

// Close modal when clicking outside
window.addEventListener('click', (e) => {
    if (e.target === cartModal) {
        cartModal.style.display = 'none';
    }
});

// Add to cart functionality
addToCartButtons.forEach(button => {
    button.addEventListener('click', async () => {
        const id = button.getAttribute('data-id');
        const name = button.getAttribute('data-name');
        
        await addToCart(id, name);
    });
});

// Add item to cart
async function addToCart(id, name) {
    try {
        const response = await fetch(`${API_BASE}cart.php`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                productId: id,
                productName: name
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            await updateCartCount();
            await updateCartDisplay();
            alert(`Added "${name}" to your cart!`);
        } else {
            alert('Failed to add item to cart: ' + data.message);
        }
    } catch (error) {
        console.error('Add to cart error:', error);
        alert('Failed to add item to cart. Please try again.');
    }
}

// Update cart count in header
async function updateCartCount() {
    try {
        const response = await fetch(`${API_BASE}cart.php`);
        const data = await response.json();
        
        if (data.success) {
            const totalItems = data.cart.reduce((total, item) => total + item.quantity, 0);
            cartCount.textContent = totalItems;
        }
    } catch (error) {
        console.error('Update cart count error:', error);
    }
}

// Update cart display in modal
async function updateCartDisplay() {
    try {
        const response = await fetch(`${API_BASE}cart.php`);
        const data = await response.json();
        
        cartItems.innerHTML = '';
        
        if (!data.success || data.cart.length === 0) {
            cartItems.innerHTML = '<div class="empty-cart">Your cart is empty</div>';
            cartTotal.textContent = '0';
            return;
        }
        
        let totalItems = 0;
        
        data.cart.forEach(item => {
            totalItems += item.quantity;
            
            const cartItem = document.createElement('div');
            cartItem.className = 'cart-item';
            cartItem.innerHTML = `
                <div class="item-details">
                    <h4>${item.product_name}</h4>
                    <div class="item-price">$${item.price.toFixed(2)}</div>
                </div>
                <div class="item-quantity">
                    <button class="quantity-btn minus" data-id="${item.id}">-</button>
                    <span class="quantity">${item.quantity}</span>
                    <button class="quantity-btn plus" data-id="${item.id}">+</button>
                </div>
                <button class="remove-item" data-id="${item.id}">&times;</button>
            `;
            
            cartItems.appendChild(cartItem);
        });
        
        cartTotal.textContent = totalItems;
        
        // Add event listeners to quantity buttons
        document.querySelectorAll('.quantity-btn.minus').forEach(button => {
            button.addEventListener('click', async () => {
                const id = button.getAttribute('data-id');
                await updateCartItem(id, -1);
            });
        });
        
        document.querySelectorAll('.quantity-btn.plus').forEach(button => {
            button.addEventListener('click', async () => {
                const id = button.getAttribute('data-id');
                await updateCartItem(id, 1);
            });
        });
        
        document.querySelectorAll('.remove-item').forEach(button => {
            button.addEventListener('click', async () => {
                const id = button.getAttribute('data-id');
                await removeFromCart(id);
            });
        });
        
    } catch (error) {
        console.error('Update cart display error:', error);
        cartItems.innerHTML = '<div class="empty-cart">Error loading cart</div>';
    }
}

// Update cart item quantity
async function updateCartItem(cartId, change) {
    try {
        // First get current quantity
        const response = await fetch(`${API_BASE}cart.php`);
        const data = await response.json();
        
        const cartItem = data.cart.find(item => item.id == cartId);
        if (!cartItem) return;
        
        const newQuantity = cartItem.quantity + change;
        
        if (newQuantity <= 0) {
            await removeFromCart(cartId);
        } else {
            const updateResponse = await fetch(`${API_BASE}cart.php`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    cartId: cartId,
                    quantity: newQuantity
                })
            });
            
            const updateData = await updateResponse.json();
            if (updateData.success) {
                await updateCartCount();
                await updateCartDisplay();
            }
        }
    } catch (error) {
        console.error('Update cart item error:', error);
    }
}

// Remove item from cart
async function removeFromCart(cartId) {
    try {
        const response = await fetch(`${API_BASE}cart.php`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                cartId: cartId,
                quantity: 0
            })
        });
        
        const data = await response.json();
        if (data.success) {
            await updateCartCount();
            await updateCartDisplay();
        }
    } catch (error) {
        console.error('Remove from cart error:', error);
    }
}

// Checkout functionality - updated to go to payment page
checkoutBtn.addEventListener('click', () => {
    cartModal.style.display = 'none';
    showPage('payment');
});

// PAYMENT SYSTEM FUNCTIONALITY
// DOM elements for payment system
const paymentMethods = document.querySelectorAll('.method');
const cardForm = document.getElementById('card-form');
const mobileForm = document.getElementById('mobile-form');
const paynowForm = document.getElementById('paynow-form');
const payButton = document.getElementById('pay-button');
const paymentSuccessMessage = document.getElementById('paymentSuccessMessage');
const paymentOverlay = document.getElementById('paymentOverlay');
const closePaymentSuccess = document.getElementById('close-payment-success');

// Form validation elements
const cardNumberInput = document.getElementById('card-number');
const expiryInput = document.getElementById('expiry-date');
const cvvInput = document.getElementById('cvv');
const cardholderInput = document.getElementById('cardholder-name');
const mobileInput = document.getElementById('mobile-number');
const transactionRefInput = document.getElementById('transaction-ref');

// Error message elements
const cardNumberError = document.getElementById('card-number-error');
const expiryError = document.getElementById('expiry-error');
const cvvError = document.getElementById('cvv-error');
const nameError = document.getElementById('name-error');
const mobileError = document.getElementById('mobile-error');
const refError = document.getElementById('ref-error');

// Current selected payment method
let selectedMethod = 'card';

// Payment method selection
paymentMethods.forEach(method => {
    method.addEventListener('click', function() {
        // Remove active class from all methods
        paymentMethods.forEach(m => m.classList.remove('active'));
        // Add active class to clicked method
        this.classList.add('active');
        
        // Get the selected method
        selectedMethod = this.getAttribute('data-method');
        
        // Show the appropriate form
        cardForm.style.display = 'none';
        mobileForm.style.display = 'none';
        paynowForm.style.display = 'none';
        
        if (selectedMethod === 'card') {
            cardForm.style.display = 'block';
            payButton.innerHTML = '<i class="fas fa-lock"></i> Complete Secure Payment';
        } else if (selectedMethod === 'paynow') {
            paynowForm.style.display = 'block';
            payButton.innerHTML = '<i class="fas fa-qrcode"></i> Confirm PayNow Payment';
        } else {
            mobileForm.style.display = 'block';
            let methodName = this.querySelector('.method-name').textContent;
            payButton.innerHTML = `<i class="fas fa-mobile-alt"></i> Confirm ${methodName} Payment`;
        }
    });
});

// Format card number with spaces
cardNumberInput.addEventListener('input', function(e) {
    let value = e.target.value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
    let formattedValue = '';
    
    for (let i = 0; i < value.length; i++) {
        if (i > 0 && i % 4 === 0) {
            formattedValue += ' ';
        }
        formattedValue += value[i];
    }
    
    e.target.value = formattedValue.substring(0, 19);
});

// Format expiry date
expiryInput.addEventListener('input', function(e) {
    let value = e.target.value.replace(/\D/g, '');
    
    if (value.length >= 2) {
        e.target.value = value.substring(0, 2) + '/' + value.substring(2, 4);
    } else {
        e.target.value = value;
    }
});

// Form validation functions
function validateCardNumber() {
    const cardNumber = cardNumberInput.value.replace(/\s/g, '');
    if (cardNumber.length !== 16 || !/^\d+$/.test(cardNumber)) {
        cardNumberError.style.display = 'block';
        return false;
    }
    cardNumberError.style.display = 'none';
    return true;
}

function validateExpiry() {
    const expiry = expiryInput.value;
    const regex = /^(0[1-9]|1[0-2])\/([0-9]{2})$/;
    
    if (!regex.test(expiry)) {
        expiryError.style.display = 'block';
        return false;
    }
    
    // Check if card is expired
    const [month, year] = expiry.split('/');
    const currentYear = new Date().getFullYear() % 100;
    const currentMonth = new Date().getMonth() + 1;
    
    if (parseInt(year) < currentYear || (parseInt(year) === currentYear && parseInt(month) < currentMonth)) {
        expiryError.textContent = 'Card has expired';
        expiryError.style.display = 'block';
        return false;
    }
    
    expiryError.style.display = 'none';
    return true;
}

function validateCVV() {
    const cvv = cvvInput.value;
    if (cvv.length !== 3 || !/^\d+$/.test(cvv)) {
        cvvError.style.display = 'block';
        return false;
    }
    cvvError.style.display = 'none';
    return true;
}

function validateCardholder() {
    if (cardholderInput.value.trim().length < 3) {
        nameError.style.display = 'block';
        return false;
    }
    nameError.style.display = 'none';
    return true;
}

function validateMobile() {
    const mobile = mobileInput.value.trim();
    if (mobile.length < 9 || !/^[0-9+\-\s]+$/.test(mobile)) {
        mobileError.style.display = 'block';
        return false;
    }
    mobileError.style.display = 'none';
    return true;
}

function validateTransactionRef() {
    const ref = transactionRefInput.value.trim();
    if (ref.length < 3) {
        refError.style.display = 'block';
        return false;
    }
    refError.style.display = 'none';
    return true;
}

// Validate form based on selected method
function validatePaymentForm() {
    if (selectedMethod === 'card') {
        return validateCardNumber() && validateExpiry() && validateCVV() && validateCardholder();
    } else if (selectedMethod === 'paynow') {
        return true; // PayNow doesn't require form validation
    } else {
        return validateMobile() && validateTransactionRef();
    }
}

// Payment button click handler
payButton.addEventListener('click', async function() {
    // First check if cart is empty
    try {
        const response = await fetch(`${API_BASE}cart.php`);
        const data = await response.json();
        
        if (!data.success || data.cart.length === 0) {
            alert('Your cart is empty. Please add items to your cart before proceeding to payment.');
            showPage('home');
            return;
        }
    } catch (error) {
        console.error('Cart check error:', error);
    }
    
    if (!validatePaymentForm()) {
        return;
    }
    
    // Simulate payment processing
    payButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing Payment...';
    payButton.disabled = true;
    
    try {
        const response = await fetch(`${API_BASE}process_payment.php`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                paymentMethod: selectedMethod,
                cardNumber: selectedMethod === 'card' ? cardNumberInput.value : '',
                expiryDate: selectedMethod === 'card' ? expiryInput.value : '',
                cvv: selectedMethod === 'card' ? cvvInput.value : '',
                cardholderName: selectedMethod === 'card' ? cardholderInput.value : '',
                mobileNumber: selectedMethod !== 'card' ? mobileInput.value : '',
                transactionRef: selectedMethod !== 'card' ? transactionRefInput.value : ''
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            // Show success message
            paymentSuccessMessage.style.display = 'block';
            paymentOverlay.style.display = 'block';
            
            // Update success message with actual data
            document.getElementById('payment-amount').textContent = `$${data.amount}`;
            document.getElementById('transaction-id').textContent = data.transactionId;
            document.getElementById('success-order-id').textContent = data.orderId;
            
            // Update cart count
            await updateCartCount();
            await updatePaymentCartSummary();
        } else {
            alert('Payment failed: ' + data.message);
        }
    } catch (error) {
        console.error('Payment error:', error);
        alert('Payment failed. Please try again.');
    } finally {
        // Reset button
        payButton.innerHTML = '<i class="fas fa-lock"></i> Complete Secure Payment';
        payButton.disabled = false;
    }
});

// Close payment success message
closePaymentSuccess.addEventListener('click', function() {
    paymentSuccessMessage.style.display = 'none';
    paymentOverlay.style.display = 'none';
    // Go back to home page
    showPage('home');
});

// Close success message when clicking overlay
paymentOverlay.addEventListener('click', function() {
    paymentSuccessMessage.style.display = 'none';
    paymentOverlay.style.display = 'none';
});

// Real-time validation for card inputs
cardNumberInput.addEventListener('blur', validateCardNumber);
expiryInput.addEventListener('blur', validateExpiry);
cvvInput.addEventListener('blur', validateCVV);
cardholderInput.addEventListener('blur', validateCardholder);
mobileInput.addEventListener('blur', validateMobile);
transactionRefInput.addEventListener('blur', validateTransactionRef);

// Initialize with card validation
validateCardNumber();
validateExpiry();
validateCVV();
validateCardholder();

// Feedback form submission
document.getElementById('feedbackForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const name = document.getElementById('name').value;
    const email = document.getElementById('email').value;
    const subject = document.getElementById('subject').value;
    const message = document.getElementById('message').value;
    const rating = document.querySelector('input[name="rating"]:checked');
    
    if (!rating) {
        alert('Please provide a rating!');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}feedback.php`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                name,
                email,
                subject,
                message,
                rating: rating.value
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            alert(`Thank you for your message, ${name}! We have received your feedback.`);
            document.getElementById('feedbackForm').reset();
        } else {
            alert('Failed to submit feedback: ' + data.message);
        }
    } catch (error) {
        console.error('Feedback error:', error);
        alert('Failed to submit feedback. Please try again.');
    }
});

// Initialize the page
document.addEventListener('DOMContentLoaded', async function() {
    // Set home as active page
    showPage('home');
    
    // Initialize cart count
    await updateCartCount();
    
    // Initialize payment cart summary
    await updatePaymentCartSummary();
});
</script>