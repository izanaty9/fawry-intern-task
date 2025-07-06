import java.time.LocalDate;
import java.util.*;

interface Shippable {
    String getName();
    double getWeight();
}

abstract class Product {
    protected String name;
    protected double price;
    protected int quantity;
    
    public Product(String name, double price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
    
    public String getName() {
        return name;
    }
    
    public double getPrice() {
        return price;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public abstract boolean isExpired();
    public abstract boolean requiresShipping();
}

class PerishableProduct extends Product {
    private LocalDate expirationDate;
    
    public PerishableProduct(String name, double price, int quantity, LocalDate expirationDate) {
        super(name, price, quantity);
        this.expirationDate = expirationDate;
    }
    
    @Override
    public boolean isExpired() {
        return LocalDate.now().isAfter(expirationDate);
    }
    
    @Override
    public boolean requiresShipping() {
        return true;
    }
}

class NonPerishableProduct extends Product {
    private boolean needsShipping;
    
    public NonPerishableProduct(String name, double price, int quantity, boolean needsShipping) {
        super(name, price, quantity);
        this.needsShipping = needsShipping;
    }
    
    @Override
    public boolean isExpired() {
        return false;
    }
    
    @Override
    public boolean requiresShipping() {
        return needsShipping;
    }
}

class ShippableProduct extends Product implements Shippable {
    private double weight;
    private LocalDate expirationDate;
    
    public ShippableProduct(String name, double price, int quantity, double weight, LocalDate expirationDate) {
        super(name, price, quantity);
        this.weight = weight;
        this.expirationDate = expirationDate;
    }
    
    public ShippableProduct(String name, double price, int quantity, double weight) {
        this(name, price, quantity, weight, null);
    }
    
    @Override
    public double getWeight() {
        return weight;
    }
    
    @Override
    public boolean isExpired() {
        return expirationDate != null && LocalDate.now().isAfter(expirationDate);
    }
    
    @Override
    public boolean requiresShipping() {
        return true;
    }
}

class Customer {
    private String name;
    private double balance;
    
    public Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }
    
    public String getName() {
        return name;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public void deductBalance(double amount) {
        this.balance -= amount;
    }
}

class CartItem {
    private Product product;
    private int quantity;
    
    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public double getTotalPrice() {
        return product.getPrice() * quantity;
    }
}

class Cart {
    private List<CartItem> items;
    
    public Cart() {
        this.items = new ArrayList<>();
    }
    
    public void add(Product product, int quantity) throws Exception {
        if (product.isExpired()) {
            throw new Exception("Cannot add expired product: " + product.getName());
        }
        
        if (quantity > product.getQuantity()) {
            throw new Exception("Insufficient quantity for product: " + product.getName() + 
                              ". Available: " + product.getQuantity() + ", Requested: " + quantity);
        }
        
        items.add(new CartItem(product, quantity));
        
        product.setQuantity(product.getQuantity() - quantity);
    }
    
    public List<CartItem> getItems() {
        return items;
    }
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    public double getSubtotal() {
        return items.stream().mapToDouble(CartItem::getTotalPrice).sum();
    }
}

class ShippingService {
    private static final double SHIPPING_RATE_PER_KG = 10.0;
    
    public static double calculateShippingFee(List<Shippable> shippableItems) {
        double totalWeight = shippableItems.stream().mapToDouble(Shippable::getWeight).sum();
        return totalWeight * SHIPPING_RATE_PER_KG;
    }
    
    public static void processShipment(List<Shippable> shippableItems) {
        if (shippableItems.isEmpty()) {
            return;
        }
        
        System.out.println("** Shipment notice **");
        double totalWeight = 0;
        
        for (Shippable item : shippableItems) {
            System.out.println("1x " + item.getName() + " " + (item.getWeight() * 1000) + "g");
            totalWeight += item.getWeight();
        }
        
        System.out.println("Total package weight " + totalWeight + "kg");
    }
}

class ECommerceSystem {
    
    public static void checkout(Customer customer, Cart cart) {
        try {
            if (cart.isEmpty()) {
                throw new Exception("Cart is empty");
            }
            
            for (CartItem item : cart.getItems()) {
                if (item.getProduct().isExpired()) {
                    throw new Exception("Product expired: " + item.getProduct().getName());
                }
                if (item.getProduct().getQuantity() < 0) {
                    throw new Exception("Product out of stock: " + item.getProduct().getName());
                }
            }
            
            double subtotal = cart.getSubtotal();
            
            List<Shippable> shippableItems = new ArrayList<>();
            for (CartItem item : cart.getItems()) {
                if (item.getProduct().requiresShipping() && item.getProduct() instanceof Shippable) {
                    for (int i = 0; i < item.getQuantity(); i++) {
                        shippableItems.add((Shippable) item.getProduct());
                    }
                }
            }
            
            double shippingFee = ShippingService.calculateShippingFee(shippableItems);
            double totalAmount = subtotal + shippingFee;
            
            if (customer.getBalance() < totalAmount) {
                throw new Exception("Customer's balance is insufficient. Required: " + totalAmount + 
                                  ", Available: " + customer.getBalance());
            }
            
            customer.deductBalance(totalAmount);
            
            ShippingService.processShipment(shippableItems);
            
            System.out.println("** Checkout receipt **");
            for (CartItem item : cart.getItems()) {
                System.out.println(item.getQuantity() + "x " + item.getProduct().getName() + 
                                 " " + (int)item.getTotalPrice());
            }
            System.out.println("----------------------");
            System.out.println("Subtotal " + (int)subtotal);
            System.out.println("Shipping " + (int)shippingFee);
            System.out.println("Amount " + (int)totalAmount);
            System.out.println("Customer balance after payment: " + customer.getBalance());
            System.out.println("=================================================");
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

public class Main {
    public static void main(String[] args) {
        System.out.println("=========== E-Commerce System Test Cases ===========n");
        
        ShippableProduct cheese = new ShippableProduct("Cheese", 100, 5, 0.2, LocalDate.now().plusDays(7));
        ShippableProduct tv = new ShippableProduct("TV", 500, 3, 5.0);
        NonPerishableProduct scratchCard = new NonPerishableProduct("Mobile Scratch Card", 50, 10, false);
        ShippableProduct biscuits = new ShippableProduct("Biscuits", 150, 4, 0.7, LocalDate.now().plusDays(30));
        
        Customer customer = new Customer("Ahmed Mohamed", 1000.0);
        
        System.out.println("=========== Test Case 1: Successful Checkout ===========");
        Cart cart1 = new Cart();
        try {
            cart1.add(cheese, 2);
            cart1.add(biscuits, 1);
            cart1.add(scratchCard, 1);
            ECommerceSystem.checkout(customer, cart1);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        System.out.println("\n=========== Test Case 2: Empty Cart ===========");
        Cart emptyCart = new Cart();
        ECommerceSystem.checkout(customer, emptyCart);
        
        System.out.println("\n=========== Test Case 3: Insufficient Balance ===========");
        Customer poorCustomer = new Customer("Eslam Zanaty", 50.0);
        Cart expensiveCart = new Cart();
        try {
            expensiveCart.add(tv, 2);
            ECommerceSystem.checkout(poorCustomer, expensiveCart);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        System.out.println("\n=========== Test Case 4: Insufficient Quantity ===========");
        Cart cart4 = new Cart();
        try {
            cart4.add(cheese, 10);
            ECommerceSystem.checkout(customer, cart4);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        System.out.println("\n=========== Test Case 5: Expired Product ===========");
        ShippableProduct expiredCheese = new ShippableProduct("Expired Cheese", 100, 5, 0.2, LocalDate.now().minusDays(1));
        Cart cart5 = new Cart();
        try {
            cart5.add(expiredCheese, 1);
            ECommerceSystem.checkout(customer, cart5);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        System.out.println("\n=========== Test Case 6: Non-Shippable Items Only ===========");
        Cart cart6 = new Cart();
        try {
            cart6.add(scratchCard, 3);
            ECommerceSystem.checkout(customer, cart6);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        System.out.println("\n=========== Test Case 7: Large Order with Shipping ===========");
        Customer Rana = new Customer("Rana Osman", 5000.0);
        Cart cart7 = new Cart();
        try {
            cart7.add(tv, 2);
            cart7.add(cheese, 1);
            ECommerceSystem.checkout(Rana, cart7);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}