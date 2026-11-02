# Despliegue de SkillBridge AI en AWS

La arquitectura prevista es: **Application Load Balancer → ECS Fargate → RDS MySQL**, con la imagen en ECR y los secretos en AWS Secrets Manager.

## 1. Preparar RDS

1. Crea una instancia RDS MySQL 8 en subredes privadas y sin acceso público.
2. Crea la base `skillbridge_ai`.
3. Ejecuta `src/main/resources/database/skillbridge_ai_18_tablas_corregida.sql` para una base nueva. Para una base ya existente, ejecuta solamente `actualizacion_chatbot_gemini.sql`.
4. En el security group de RDS permite TCP 3306 **solo desde el security group del servicio ECS**.
5. Usa esta URL en ECS (reemplaza el endpoint):

   ```text
   jdbc:mysql://ENDPOINT-RDS:3306/skillbridge_ai?serverTimezone=America/Lima&sslMode=REQUIRED&allowPublicKeyRetrieval=false
   ```

## 2. Guardar secretos

Crea tres secretos independientes o un secreto JSON con estas claves:

- `GEMINI_API_KEY`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

No coloques estos valores en la imagen, la definición de tarea, Git ni CloudWatch. La función de ejecución de ECS necesita permiso `secretsmanager:GetSecretValue`. Cuando rotes un secreto, fuerza un nuevo despliegue del servicio para que las tareas reciban la versión nueva.

## 3. Construir y publicar en ECR

```bash
docker build -t skillbridge-ai .
aws ecr get-login-password --region REGION | docker login --username AWS --password-stdin CUENTA.dkr.ecr.REGION.amazonaws.com
docker tag skillbridge-ai:latest CUENTA.dkr.ecr.REGION.amazonaws.com/skillbridge-ai:latest
docker push CUENTA.dkr.ecr.REGION.amazonaws.com/skillbridge-ai:latest
```

## 4. Definición de tarea ECS Fargate

Configura el contenedor con puerto `8080`, logs `awslogs` y:

Variables no sensibles:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://ENDPOINT-RDS:3306/skillbridge_ai?serverTimezone=America/Lima&sslMode=REQUIRED&allowPublicKeyRetrieval=false
SPRING_AI_MODEL_CHAT=google-genai
GEMINI_MODEL=gemini-3.5-flash-lite
UPLOAD_DIR=/app/uploads
JPA_SHOW_SQL=false
TZ=America/Lima
```

Secretos inyectados desde Secrets Manager:

```text
GEMINI_API_KEY
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

Usa al menos 1 vCPU / 2 GB para el primer despliegue y ajusta después de medir. La aplicación corre como usuario no root y expone `/actuator/health`.

## 5. Red y balanceador

- Tareas ECS en subredes privadas; ALB en subredes públicas.
- Security group del ALB: entrada HTTPS 443 desde Internet.
- Security group de ECS: entrada 8080 solo desde el security group del ALB.
- Target group de tipo `ip`, puerto 8080 y health check `GET /actuator/health`.
- Termina TLS en el ALB con un certificado ACM y redirige HTTP 80 a HTTPS 443.

## 6. Persistencia de archivos

El volumen `uploads` de Docker es suficiente para local. En Fargate el disco de la tarea es efímero; para credenciales cargadas por usuarios monta EFS en `/app/uploads` o migra ese módulo a S3 antes de producción.

## 7. Prueba local con Docker

```bash
copy .env.example .env
# Edita .env y define GEMINI_API_KEY con una clave de Google AI Studio
docker compose up --build
```

Abre `http://localhost:8080`, inicia sesión con un usuario demo y usa el botón **Asistente IA** del encabezado. Para detener el entorno:

```bash
docker compose down
```

`docker compose down -v` también borra la base local y los archivos del volumen; úsalo solo si quieres reiniciar todos los datos.
