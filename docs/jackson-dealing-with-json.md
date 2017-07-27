# Jackson: Dealing with Json

A common use case for Indoqa-Boot is to provide Json resources. For that purpose the simplest approach is to use [AbstractJsonResourcesBase](https://www.javadoc.io/page/com.indoqa/indoqa-boot/latest/com/indoqa/boot/json/resources/AbstractJsonResourcesBase.html) which uses the [Jackson](https://github.com/FasterXML/jackson) object mapper.

```java
public class TestJsonResource extends AbstractJsonResourcesBase {

    @PostConstruct
    public void mount() {
        this.get("/test", (req, res) -> new TestObject("test-object"));
    }

    public static class TestObject {

        private String name;

        public TestObject(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}
```

`AbstractJsonResourcesBase` provides route mapping methods that set the correct response type and a Json response transformer. Of course can use the default Spark route mapping methods but then you have to take care of the response type and the response transformer for every route mapping yourself.

## Custom Json response transformer

If you want to customize the Json response transformer, you can provide your implementation by overriding the method `getJsonTransformerClass` of [AbstractIndoqaBootApplication](https://www.javadoc.io/page/com.indoqa/indoqa-boot/latest/com/indoqa/boot/application/AbstractIndoqaBootApplication.html). A good starting point for your implementation is the [AbstractJacksonTransformer](https://www.javadoc.io/page/com.indoqa/indoqa-boot/latest/com/indoqa/boot/json/transformer/AbstractJacksonTransformer) which provides a template method `configure` where you can configure the Jackson object mapper.
