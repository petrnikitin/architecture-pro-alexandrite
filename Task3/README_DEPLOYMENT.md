# Инструкция по развёртыванию сервисов с трейсингом

## Что реализовано

Два микросервиса на Java Spring Boot с OpenTelemetry трейсингом:
- **Service A (Orders)** - GET `/` - создаёт заказ и вызывает Service B
- **Service B (Price Calculator)** - GET `/calculate-price` - рассчитывает цену

Трейсинг: Service A → Service B проходит как **один trace** через Jaeger.

## Требования

- Minikube
- kubectl
- Docker
- Java 17+ (для локальной разработки, опционально)

## Шаг 1: Запуск Minikube

```bash
minikube start --addons=ingress
```

Проверка:
```bash
kubectl get nodes
```

## Шаг 2: Установка cert-manager

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.3/cert-manager.yaml
```

Ждём готовности:
```bash
kubectl wait --for=condition=ready pod -l app=cert-manager -n cert-manager --timeout=300s
```

## Шаг 3: Установка Jaeger Operator

```bash
kubectl create namespace observability
kubectl create -f https://github.com/jaegertracing/jaeger-operator/releases/download/v1.51.0/jaeger-operator.yaml -n observability
```

Ждём готовности оператора:
```bash
kubectl wait --for=condition=ready pod -l name=jaeger-operator -n observability --timeout=300s
```

## Шаг 4: Развёртывание Jaeger Instance

```bash
kubectl apply -f k8s/jaeger-instance.yaml
```

Проверка:
```bash
kubectl get jaeger
kubectl get pods | grep simplest
```

Должны появиться pods: `simplest-xxx` (collector, query, agent).

## Шаг 5: Сборка Docker образов сервисов

**Важно:** Используем Minikube Docker daemon для сборки образов внутри кластера:

```bash
eval $(minikube docker-env)
```

Сборка Service A:
```bash
cd services/service-a
docker build -t service-a:latest .
```

Сборка Service B:
```bash
cd ../service-b
docker build -t service-b:latest .
cd ../..
```

Проверка образов:
```bash
docker images | grep service
```

## Шаг 6: Развёртывание сервисов в Kubernetes

```bash
kubectl apply -f k8s/services.yaml
```

Проверка:
```bash
kubectl get pods
kubectl get services
```

Должны быть запущены:
- `service-a-xxx` (Running)
- `service-b-xxx` (Running)

Логи для отладки:
```bash
kubectl logs -f deployment/service-a
kubectl logs -f deployment/service-b
```

## Шаг 7: Тестирование трейсинга

### Вызов Service A (который вызывает Service B)

```bash
kubectl exec -it $(kubectl get pods -l app=service-a -o jsonpath='{.items[0].metadata.name}') -- wget -qO- http://service-a:8080
```

Ожидаемый ответ:
```
Order created successfully! Order ID: ORD-12345, Price: $5432
```

Сделайте несколько вызовов для генерации traces:
```bash
for i in {1..5}; do
  kubectl exec -it $(kubectl get pods -l app=service-a -o jsonpath='{.items[0].metadata.name}') -- wget -qO- http://service-a:8080
  echo ""
done
```

## Шаг 8: Открытие Jaeger UI

```bash
kubectl port-forward svc/simplest-query 16686:16686
```

Откройте в браузере: **http://localhost:16686**

### Поиск трейсов

1. В Jaeger UI выберите **Service: service-a**
2. Нажмите **Find Traces**
3. Вы увидите список трейсов
4. Кликните на любой трейс