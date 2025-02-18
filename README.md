# 📈 Trading System (Router - Broker - Market)

## 📌 Overview
This project is a **high-performance trading simulation system** that follows the **Financial Information eXchange (FIX) protocol**. It consists of three key components:

- **Broker**: Sends buy/sell orders.
- **Router**: Routes messages between Brokers and Markets.
- **Market**: Processes orders and executes or rejects them.

All components communicate using **non-blocking sockets (Java NIO)**, ensuring asynchronous, multi-threaded message processing.

---

## 🎯 Why Build This Project?
Developing such a system helps simulate real-world financial trading, allowing developers to:
- Understand the **core mechanisms of electronic trading**.
- Gain hands-on experience with **high-frequency trading architectures**.
- Work with **low-latency, event-driven systems**.
- Implement **multi-threaded network applications** with **asynchronous communication**.

This project serves as a foundation for developing robust financial applications that require **secure, high-speed data transmission**.

---

## ❓ What is FIX Protocol?
The **Financial Information eXchange (FIX)** protocol is a **global standard for electronic trading communication**. It is widely used by financial institutions, stock exchanges, and trading firms.

### 📜 Key Features of FIX:
- **Standardized format** for financial message exchange.
- **Low-latency & high-performance** messaging.
- **Ensures integrity** via message validation (checksum, sequencing, etc.).
- **Used in equities, derivatives, FX, and more.**

### 🔧 How FIX is Used Here:
Our trading system follows a simplified **FIX protocol** to structure messages sent between **Brokers, Router, and Market**. This ensures reliable and structured trade execution.

---

## 🚀 Technologies Used
- **Java 17+** (Latest LTS version)
- **Java NIO (Non-blocking Sockets)**
- **Multi-threading & Concurrency**
- **Maven for Dependency Management**
- **FIX Protocol** for financial message exchange

---

## 🔥 FIX Protocol Implementation
Our trading system follows a simplified **FIX (Financial Information eXchange) protocol** for message communication.

**Message Format:**
```
<ID>|<OrderType>|<Instrument>|<Quantity>|<Market>|<Price>|<Checksum>
```
- `ID` → Assigned by Router
- `OrderType` → BUY / SELL
- `Instrument` → Stock or commodity name
- `Quantity` → Number of units
- `Market` → Target market
- `Price` → Order price
- `Checksum` → Ensures message integrity

Checksum is calculated using **mod 256 summation**.

---

## 🔧 How It Works
1. **Broker connects to Router** and receives a unique ID.
2. **Market connects to Router** and also gets an assigned ID.
3. **Broker sends orders to Router**, formatted in FIX message style.
4. **Router validates checksum** and forwards the message to the appropriate Market.
5. **Market processes the order**:
    - If successful, sends an `Executed` response.
    - Otherwise, sends a `Rejected` response.
6. **Broker receives Market response** and logs the trade outcome.

---

## ⚙️ Setup & Run Instructions
### 1️⃣ Clone the Repository
```sh
git clone https://github.com/yourusername/trading-system.git
cd trading-system
```

### 2️⃣ Build the Project
```sh
mvn clean package
```

### 3️⃣ Start the Components
Run each component in a separate terminal:
#### Start Router
```sh
java -jar router/target/router.jar
```
#### Start Market
```sh
java -jar market/target/market.jar
```
#### Start Broker
```sh
java -jar broker/target/broker.jar
```

---

## 📚 Code Breakdown
### 🏦 Broker (`Broker.java`)
- Connects to Router via `SocketManager`.
- Accepts user input for orders.
- Sends orders in **FIX format**.
- Receives execution results from Market.

### 🔀 Router (`Router.java`)
- Assigns unique IDs to Brokers & Markets.
- Forwards messages based on **routing table**.
- **Validates checksum** before forwarding messages.

### 📊 Market (`Market.java`)
- Receives orders from Router.
- Processes Buy/Sell requests.
- Maintains an **instrument database**.
- Sends responses to Brokers.

---

## 🚀 Future Enhancements
- ✅ **Database Integration**: Store transaction logs.
- ✅ **Fail-over Mechanism**: Restore transactions if a component crashes.
- ✅ **Graphical Dashboard**: Visualize trade execution statistics.

---


## 📩 Contact
- 📧 Email: erdogancayir02@gmail.com
- 🐙 GitHub: [erdogancayir](https://github.com/erdogancayir)

---


# AsynchronousSocketChannel mı yoksa Selector & Executor mü?

Bu dokümanda, **AsynchronousSocketChannel** ve **Selector & Executor** kullanımı karşılaştırılarak, hangi yöntemin belirli senaryolar için daha uygun olduğu açıklanmaktadır.

## 📌 Proje Gereksinimleri

- **Non-blocking** socket kullanımı zorunlu.
- **Java Executor Framework** kullanılmalı (thread yönetimi için).
- **Performans kritik** (yüksek hızlı mesaj işleme gereksinimi var).

## ⚖️ Seçeneklerin Karşılaştırılması

| Özellik | AsynchronousSocketChannel | Selector & Executor (NIO) |
|---------|--------------------------|--------------------------|
| **Kod Karmaşıklığı** | Daha basit, çünkü Java'nın `CompletableFuture` ve `CompletionHandler` API'leri ile doğal async programlama yapabiliyorsun. | Daha karmaşık, çünkü `Selector` ile olay bazlı bir yapı kurmak gerekiyor. |
| **Performans (Düşük Latency)** | Daha iyi ölçeklenebilirlik sağlar, ancak çok yoğun I/O trafiğinde thread-per-connection modeli nedeniyle CPU tıkanabilir. | `Selector` ile çalışırken tek thread tüm bağlantıları yönetir. Büyük ölçekli sistemlerde daha performanslı olabilir. |
| **Thread Yönetimi** | Java'nın `AsynchronousChannelGroup` ile yönetilebilir, ancak thread pool boyutu iyi ayarlanmazsa darboğaz yaratabilir. | `ExecutorService` kullanımı ile esnek thread yönetimi sağlanabilir. |
| **Uygulama için Uygunluk** | **FIX protokolü** gibi mesaj trafiği yoğun olan uygulamalarda genellikle kullanılmaz. | **Finansal işlemler, FIX Router** gibi uygulamalar için en iyi yöntemdir. |

## 🚀 Bu Projede Hangi Yöntem Seçilmeli?

### ✅ **Selector + ExecutorService Daha Uygun!**

Çünkü:
- **FIX mesajları düşük gecikme ile işlenmeli** ve tek bir thread tüm bağlantıları yönetebilir.
- **Java'nın Selector API'si** ile çok sayıda bağlantıyı aynı anda yönetebilirsin.
- `AsynchronousSocketChannel`, genellikle **daha az bağlantı ve daha karmaşık async işlem gerektiren** yerlerde kullanılır. Ancak burada router, **binlerce bağlantıyı verimli yönetmeli**, bu yüzden **Selector** daha iyi bir tercih olur.

## 📌 Ne Yapılmalı?

- **Mevcut broker kodun iyi bir başlangıç**, çünkü `Selector` kullanıyor.
- **Router bileşeni de `Selector` kullanmalı** ve tüm broker & market bağlantılarını **tek bir event loop içinde yönetmeli**.
- **`ExecutorService` mesaj işleme için kullanılmalı**, ancak **I/O işlemleri `Selector` tarafından yönetilmeli**.

## 🎯 Sonuç

### **Selector & ExecutorService en iyi seçim!**

✅ **AsynchronousSocketChannel**, finansal trading sistemleri için uygun değil. Çünkü **FIX gibi uygulamalar düşük latency ve yüksek throughput için optimize edilmeli**.

Bu yüzden, **Selector ve Executor tabanlı bir router mimarisi kurmalısın!** 🚀
