package com.cg.order.orderservice.serviceimpl;

import com.cg.order.orderservice.client.AuthClient;
import com.cg.order.orderservice.client.BookClient;
import com.cg.order.orderservice.client.CartClient;
import com.cg.order.orderservice.client.NotificationClient;
import com.cg.order.orderservice.client.WalletClient;
import com.cg.order.orderservice.dto.BookResponse;
import com.cg.order.orderservice.entity.Address;
import com.cg.order.orderservice.entity.Order;
import com.cg.order.orderservice.repository.AddressRepository;
import com.cg.order.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private BookClient bookClient;

    @Mock
    private CartClient cartClient;

    @Mock
    private WalletClient walletClient;

    @Mock
    private AuthClient authClient;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Address mockAddress;
    private Order mockOrder;
    private BookResponse mockBook;
    private List<Map<String, Object>> mockCartItems;
    private Map<String, Object> mockWallet;

    @BeforeEach
    void setUp() {
        mockAddress = new Address();
        mockAddress.setAddressId(1);
        mockAddress.setCustomerId(1);
        mockAddress.setFullName("Test User");
        mockAddress.setCity("Delhi");
        mockAddress.setPincode("110001");

        mockBook = new BookResponse();
        mockBook.setBookId(1);
        mockBook.setTitle("Clean Code");
        mockBook.setPrice(499.0);
        mockBook.setStock(10);

        mockOrder = new Order();
        mockOrder.setOrderId(1);
        mockOrder.setUserId(1);
        mockOrder.setBookId(1);
        mockOrder.setBookTitle("Clean Code");
        mockOrder.setBookPrice(499.0);
        mockOrder.setQuantity(2);
        mockOrder.setAmountPaid(998.0);
        mockOrder.setModeOfPayment("COD");
        mockOrder.setOrderStatus("Placed");
        mockOrder.setAddress(mockAddress);

        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("bookId", 1);
        cartItem.put("quantity", 2);
        mockCartItems = new ArrayList<>(List.of(cartItem));

        Map<String, Object> cartBody = new HashMap<>();
        cartBody.put("items", mockCartItems);

        mockWallet = new HashMap<>();
        mockWallet.put("currentBalance", 5000.0);

        // Default stub for authClient
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("email", "test@example.com");
        lenient().when(authClient.getUserProfile(anyString(), anyInt()))
                .thenReturn(userProfile);

        // Default stub for cartClient
        lenient().when(cartClient.getCartByUserId(anyInt()))
                .thenReturn(cartBody);
    }

    // ─── Place COD Order ──────────────────────────────────────────────────────

    @Test
    void placeOrder_COD_Success() {
        when(addressRepository.findById(1))
                .thenReturn(Optional.of(mockAddress));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

        List<Order> orders = orderService.placeOrder(1, 1, "Bearer token");

        assertNotNull(orders);
        assertFalse(orders.isEmpty());
        assertEquals("COD", orders.get(0).getModeOfPayment());
        verify(bookClient).updateStock(eq(1), eq(8));
        verify(cartClient).clearCart(1);
    }

    @Test
    void placeOrder_AddressNotFound_ThrowsException() {
        when(addressRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> orderService.placeOrder(1, 99, "Bearer token"));
    }

    @Test
    void placeOrder_EmptyCart_ThrowsException() {
        Map<String, Object> emptyCart = new HashMap<>();
        emptyCart.put("items", new ArrayList<>());

        when(addressRepository.findById(1))
                .thenReturn(Optional.of(mockAddress));
        when(cartClient.getCartByUserId(1)).thenReturn(emptyCart);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.placeOrder(1, 1, "Bearer token"));
        assertTrue(ex.getMessage().contains("Cart is empty"));
    }

    @Test
    void placeOrder_InsufficientStock_ThrowsException() {
        mockBook.setStock(1);

        when(addressRepository.findById(1))
                .thenReturn(Optional.of(mockAddress));
        when(bookClient.getBookById(1)).thenReturn(mockBook);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.placeOrder(1, 1, "Bearer token"));
        assertTrue(ex.getMessage().contains("Insufficient stock"));
    }

    // ─── Wallet Payment ───────────────────────────────────────────────────────

    @Test
    void onlinePayment_Wallet_Success() {
        when(addressRepository.findById(1))
                .thenReturn(Optional.of(mockAddress));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(walletClient.getWallet(1)).thenReturn(mockWallet);
        when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

        List<Order> orders = orderService.onlinePayment(1, 1, "Bearer token");

        assertNotNull(orders);
        assertFalse(orders.isEmpty());
        assertEquals("WALLET", orders.get(0).getModeOfPayment());
        verify(walletClient).payMoney(eq(1), eq(998.0), anyInt());
        verify(cartClient).clearCart(1);
    }

    @Test
    void onlinePayment_InsufficientBalance_ThrowsException() {
        mockWallet.put("currentBalance", 100.0);

        when(addressRepository.findById(1))
                .thenReturn(Optional.of(mockAddress));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(walletClient.getWallet(1)).thenReturn(mockWallet);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.onlinePayment(1, 1, "Bearer token"));
        assertTrue(ex.getMessage().contains("Insufficient wallet balance"));
    }

    @Test
    void onlinePayment_WalletNotFound_ThrowsException() {
        when(addressRepository.findById(1))
                .thenReturn(Optional.of(mockAddress));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(walletClient.getWallet(1)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.onlinePayment(1, 1, "Bearer token"));
        assertTrue(ex.getMessage().contains("Wallet not found"));
    }

    // ─── Razorpay Order ───────────────────────────────────────────────────────

    @Test
    void placeRazorpayOrder_Success() {
        when(addressRepository.findById(1))
                .thenReturn(Optional.of(mockAddress));
        when(bookClient.getBookById(1)).thenReturn(mockBook);
        when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

        List<Order> orders = orderService.placeRazorpayOrder(
                1, 1, "pay_123", "Bearer token");

        assertNotNull(orders);
        assertFalse(orders.isEmpty());
        verify(bookClient).updateStock(eq(1), eq(8));
        verify(cartClient).clearCart(1);
    }

    // ─── Cancel Order ─────────────────────────────────────────────────────────

    @Test
    void cancelOrder_Success() {
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        Order result = orderService.cancelOrder(1, 1, "Bearer token");

        assertNotNull(result);
        assertEquals("Cancelled", result.getOrderStatus());
    }

    @Test
    void cancelOrder_WrongUser_ThrowsException() {
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(mockOrder));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.cancelOrder(1, 99, "Bearer token"));
        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    void cancelOrder_DeliveredOrder_ThrowsException() {
        mockOrder.setOrderStatus("Delivered");

        when(orderRepository.findById(1))
                .thenReturn(Optional.of(mockOrder));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.cancelOrder(1, 1, "Bearer token"));
        assertTrue(ex.getMessage().contains("Cannot cancel"));
    }

    @Test
    void cancelOrder_WalletOrder_TriggersRefund() {
        mockOrder.setModeOfPayment("WALLET");

        when(orderRepository.findById(1))
                .thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        orderService.cancelOrder(1, 1, "Bearer token");

        verify(walletClient).refundMoney(eq(1), eq(998.0));
    }

    // ─── Change Status ────────────────────────────────────────────────────────

    @Test
    void changeStatus_Success() {
        when(orderRepository.findById(1))
                .thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        Order result = orderService.changeStatus(1, "Dispatched", "Bearer token");

        assertNotNull(result);
        assertEquals("Dispatched", result.getOrderStatus());
    }

    @Test
    void changeStatus_CancelledWalletOrder_TriggersRefund() {
        mockOrder.setModeOfPayment("WALLET");

        when(orderRepository.findById(1))
                .thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        orderService.changeStatus(1, "Cancelled", "Bearer token");

        verify(walletClient).refundMoney(eq(1), eq(998.0));
    }

    @Test
    void changeStatus_OrderNotFound_ThrowsException() {
        when(orderRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> orderService.changeStatus(99, "Dispatched", "Bearer token"));
    }

    // ─── Delete Order ─────────────────────────────────────────────────────────

    @Test
    void deleteOrder_Success() {
        when(orderRepository.existsById(1)).thenReturn(true);

        assertDoesNotThrow(() -> orderService.deleteOrder(1));
        verify(orderRepository).deleteById(1);
    }

    @Test
    void deleteOrder_NotFound_ThrowsException() {
        when(orderRepository.existsById(99)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> orderService.deleteOrder(99));
    }

    // ─── Get Orders ───────────────────────────────────────────────────────────

    @Test
    void getAllOrders_ReturnsList() {
        when(orderRepository.findAll()).thenReturn(List.of(mockOrder));

        List<Order> orders = orderService.getAllOrders();

        assertEquals(1, orders.size());
    }

    @Test
    void getOrderByUserId_ReturnsList() {
        when(orderRepository.findByUserId(1)).thenReturn(List.of(mockOrder));

        List<Order> orders = orderService.getOrderByUserId(1);

        assertEquals(1, orders.size());
        assertEquals(1, orders.get(0).getUserId());
    }

    @Test
    void getOrderById_ReturnsOrder() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(mockOrder));

        Optional<Order> result = orderService.getOrderById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getOrderId());
    }

    @Test
    void getOrderById_NotFound_ReturnsEmpty() {
        when(orderRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Order> result = orderService.getOrderById(99);

        assertFalse(result.isPresent());
    }

    // ─── Address ──────────────────────────────────────────────────────────────

    @Test
    void storeAddress_Success() {
        when(addressRepository.save(any(Address.class)))
                .thenReturn(mockAddress);

        Address result = orderService.storeAddress(mockAddress);

        assertNotNull(result);
        assertEquals("Delhi", result.getCity());
    }

    @Test
    void getAddressByCustomerId_ReturnsList() {
        when(addressRepository.findByCustomerId(1))
                .thenReturn(List.of(mockAddress));

        List<Address> addresses = orderService.getAddressByCustomerId(1);

        assertEquals(1, addresses.size());
    }

    @Test
    void getAllAddresses_ReturnsList() {
        when(addressRepository.findAll()).thenReturn(List.of(mockAddress));

        List<Address> addresses = orderService.getAllAddresses();

        assertEquals(1, addresses.size());
    }
}