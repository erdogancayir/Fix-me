<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Trading Simülasyon Sistemi</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; }
        code { background: #f4f4f4; padding: 2px 4px; border-radius: 4px; }
    </style>
</head>
<body>
    <h1>Trading Simülasyon Sistemi</h1>
    <p>FIX protokolü ile yüksek performanslı, non-blocking bir trading simülasyonu.</p>
    
    <h2>Proje Genel Bakış</h2>
    <p>Bu proje elektronik trading sistemlerini simüle eder ve aşağıdaki bileşenleri içerir:</p>
    <ul>
        <li><strong>Piyasa (Market):</strong> Alım/satım emirlerini işler.</li>
        <li><strong>Broker:</strong> Emirleri Router üzerinden piyasaya iletir.</li>
        <li><strong>Router:</strong> Broker ve Market bileşenleri arasındaki mesaj trafiğini yönlendirir.</li>
    </ul>
    
    <h2>Teknik Yapı</h2>
    <ul>
        <li><strong>Java NIO</strong> - Non-blocking soketler için <code>Selector</code> ve <code>SocketChannel</code>.</li>
        <li><strong>ExecutorService</strong> - Mesaj işleme için çoklu iş parçacığı yönetimi.</li>
        <li><strong>Maven</strong> - Multi-module proje yapısı.</li>
        <li><strong>FIX Protokolü</strong> - Standart finansal mesaj formatı.</li>
    </ul>
    
    <h2>Hangi Yöntemi Kullanmalıyız?</h2>
    <p><strong>Selector + ExecutorService</strong> en iyi tercihtir çünkü:</p>
    <ul>
        <li>FIX mesajları düşük gecikme ile işlenmeli ve tek bir thread tüm bağlantıları yönetebilir.</li>
        <li>Java'nın <code>Selector</code> API'si çok sayıda bağlantıyı aynı anda yönetmek için optimize edilmiştir.</li>
        <li>AsynchronousSocketChannel, az bağlantı gerektiren sistemler için iyidir; ancak burada binlerce bağlantıyı yönetmek gerektiği için Selector daha iyi bir tercihtir.</li>
    </ul>
    
    <h2>Nasıl Derlenir?</h2>
    <pre><code>mvn clean package</code></pre>
    <p>Bu işlem her bileşen için çalıştırılabilir JAR dosyaları üretir.</p>
    
    <h2>Nasıl Çalıştırılır?</h2>
    <p>Router'ı başlat:</p>
    <pre><code>java -jar router.jar</code></pre>
    <p>Broker'ı başlat:</p>
    <pre><code>java -jar broker.jar</code></pre>
    <p>Piyasayı başlat:</p>
    <pre><code>java -jar market.jar</code></pre>
    
    <h2>Mimari</h2>
    <p>Mesajlar non-blocking soketler üzerinden yönlendirilir ve Chain-of-Responsibility deseni kullanılır.</p>
    
    <h2>Mesaj Formatı</h2>
    <p>Tüm mesajlar FIX formatında olup, aşağıdaki zorunlu alanlara sahiptir:</p>
    <pre><code>
BrokerID|EmirTürü|Enstrüman|Miktar|Piyasa|Fiyat|Checksum
    </code></pre>
    
    <h2>Lisans</h2>
    <p>MIT Lisansı</p>
</body>
</html>
