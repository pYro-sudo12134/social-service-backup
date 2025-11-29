build:
	docker build -t pyrodocker1/user-service:latest ./user-service
	docker build -t pyrodocker1/image-service:latest ./image-service
	docker build -t pyrodocker1/comment-like-service:latest ./comment-like-service
	docker build -t pyrodocker1/social-activity-service:latest ./activity-service
	docker build -t pyrodocker1/api-gateway:latest ./api-gateway
	docker build -t pyrodocker1/photo-sharing-frontend:latest ./photo-sharing-app

push: build
	docker push pyrodocker1/user-service:latest
	docker push pyrodocker1/image-service:latest
	docker push pyrodocker1/comment-like-service:latest
	docker push pyrodocker1/social-activity-service:latest
	docker push pyrodocker1/api-gateway:latest
	docker push pyrodocker1/photo-sharing-frontend:latest

deploy: push
	terraform init
	terraform apply -auto-approve

kind-load: build
	kind load docker-image postgres:15-alpine --name desktop
	kind load docker-image redis:7.2-alpine --name desktop
	kind load docker-image confluentinc/cp-kafka:7.8.0 --name desktop
	kind load docker-image confluentinc/cp-zookeeper:7.8.0 --name desktop
	kind load docker-image mongo:6.0 --name desktop
	kind load docker-image prom/prometheus:v3.6.0 --name desktop
	kind load docker-image grafana/grafana:main --name desktop
	kind load docker-image pyrodocker1/activity-service:1.0 --name desktop
	kind load docker-image pyrodocker1/api-gateway:1.0 --name desktop
	kind load docker-image pyrodocker1/comment-like-service:1.0 --name desktop
	kind load docker-image pyrodocker1/image-service:1.0 --name desktop
	kind load docker-image pyrodocker1/user-service:1.0 --name desktop
	kind load docker-image pyrodocker1/photo-sharing-frontend:1.0 --name desktop

hand-deploy: build
	echo "Deploying all Kubernetes resources in correct order..."

	echo "Step 1: Applying base cluster resources..."
	kubectl apply -f ./k8s/namespace.yaml
	kubectl apply -f ./k8s/rbac.yaml
	kubectl apply -f ./k8s/service-accounts.yaml
	kubectl apply -f ./k8s/secrets.yaml

	echo "Step 2: Applying limits and policies..."
	kubectl apply -f ./k8s/limit-range.yaml
	kubectl apply -f ./k8s/network-policies.yaml

	echo "Step 3: Applying storage systems..."
	kubectl apply -f ./k8s/mongodb-ss.yaml
	kubectl apply -f ./k8s/postgres-ss.yaml
	kubectl apply -f ./k8s/redis-k8s.yaml
	kubectl apply -f ./k8s/kafka-k8s.yaml
	kubectl apply -f ./k8s/localstack-k8s.yaml

	echo "Waiting for storage systems to be ready..."
	kubectl wait --for=condition=ready pod -l app=postgres --timeout=180s
	kubectl wait --for=condition=ready pod -l app=redis --timeout=120s
	kubectl wait --for=condition=ready pod -l app=mongodb --timeout=180s

	echo "Step 4: Applying monitoring..."
	kubectl apply -f ./k8s/prometheus-k8s.yaml
	kubectl apply -f ./k8s/grafana-k8s.yaml

	echo "Step 5: Applying business services..."
	kubectl apply -f ./k8s/user-service-k8s.yaml
	kubectl apply -f ./k8s/image-service-k8s.yaml
	kubectl apply -f ./k8s/activity-service-k8s.yaml
	kubectl apply -f ./k8s/comment-like-service-k8s.yaml

	echo "Step 6: Applying gateway and frontend..."
	kubectl apply -f ./k8s/api-gateway-k8s.yaml
	kubectl apply -f ./k8s/front-k8s.yaml

	echo "Step 7: Applying ingress and availability policies..."
	if ! kubectl get namespace ingress-nginx > /dev/null 2>&1; then
	  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
	  kubectl wait --namespace ingress-nginx \
		--for=condition=ready pod \
		--selector=app.kubernetes.io/component=controller \
		--timeout=120s
	fi

	kubectl apply -f ./k8s/social-service-ingress.yaml
	kubectl apply -f ./k8s/social-services-pdb.yaml
	kubectl apply -f ./k8s/social-services-hpa.yaml

	#echo "Step 8: Applying backups..."
	#kubectl apply -f ./k8s/backups.yaml

	echo "Waiting for main services to be ready..."
	kubectl wait --for=condition=ready pod -l app=user-service --timeout=120s
	kubectl wait --for=condition=ready pod -l app=api-gateway --timeout=120s
	
	kubectl get pods,svc,ingress -A

