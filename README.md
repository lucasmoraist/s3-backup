# S3 Backup Service 📦☁️

Este é um serviço desenvolvido em **Java 21** com **Spring Boot 4** e **Spring Batch**, responsável por realizar backups automáticos dos dados de usuários do banco de dados **PostgreSQL** e enviá-los para um bucket no **Amazon S3**.

O serviço utiliza processamento em lote (Batch Processing) para garantir performance e baixo consumo de memória, mesmo com grandes volumes de dados, e agendamento flexível via CRON.

## 🚀 Tecnologias

* **Java 21**
* **Spring Boot 4.0.2**
* **Spring Batch** (Leitura e Escrita otimizada em chunks)
* **Spring Data JPA** (Persistência)
* **AWS SDK** (Integração com S3)
* **PostgreSQL** (Banco de Dados)
* **Docker & Docker Compose** (Ambiente de desenvolvimento)

---

## 🛠️ Configuração da AWS (IAM e S3)

Para que a aplicação consiga enviar arquivos para a AWS, você precisa configurar um Bucket e um usuário com permissões específicas.

### 1. Criar o Bucket S3
1.  Acesse o [Console da AWS S3](https://s3.console.aws.amazon.com/).
2.  Clique em **Create bucket**.
3.  **Bucket name:** Escolha um nome único (ex: `meu-backup-users-2026`).
4.  **AWS Region:** Escolha a região mais próxima (ex: `us-east-1` ou `sa-east-1`).
5.  Mantenha as configurações padrão de bloqueio de acesso público (Block Public Access) ativadas para segurança.
6.  Clique em **Create bucket**.

### 2. Criar Política de Segurança (IAM)
Vamos criar uma permissão que dá acesso *apenas* ao bucket de backup, seguindo o princípio do menor privilégio.

1.  Acesse o [Console IAM](https://console.aws.amazon.com/iam/).
2.  No menu lateral, clique em **Policies** > **Create policy**.
3.  Clique na aba **JSON** e cole o conteúdo abaixo (altere `NOME-DO-SEU-BUCKET`):

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "PermitirUploadBackup",
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::NOME-DO-SEU-BUCKET",
                "arn:aws:s3:::NOME-DO-SEU-BUCKET/*"
            ]
        }
    ]
}
```
4. Dê um nome para a política (ex: S3BackupPolicy) e crie-a.

### 3. Criar Usuário e Gerar Chaves

1. No menu do IAM, vá em **Users** > **Create user.** 
2. Nome: `s3-backup-agent`. 
3. Em **Permissions options**, selecione **Attach policies directly.** 
4. Busque e selecione a política criada no passo anterior (`S3BackupPolicy`). 
5. Finalize a criação do usuário. 
6. Clique no usuário criado, vá na aba **Security credentials.** 
7. Em **access keys**, clique em **Create access key**. 
8. Escolha **Application running outside AWS** > Next. 
9. Copie a **Access Key** e a **Secret Access Key**. Guarde-as, você não poderá ver a Secret Key novamente!

---

## ⚙️ Configuração do Projeto
Crie um arquivo `.env` na raiz do projeto ou configure as variáveis de ambiente no seu sistema operacional/container. Use o arquivo `.env.example` como base.

````env
# Credenciais da AWS (Geradas no passo anterior)
AWS_ACCESS_KEY=SUA_ACCESS_KEY_AQUI
AWS_SECRET_KEY=SUA_SECRET_KEY_AQUI
AWS_REGION=us-east-1  # A mesma região onde criou o bucket
S3_BUCKET_NAME=NOME-DO-SEU-BUCKET

# Agendamento do Backup
# Opções: 'minute', 'daily', 'weekly', 'monthly', 'annual'
# Ou uma expressão CRON customizada: "0 0 12 * * *" (Meio-dia)
BACKUP_CRON=minute
````

---
## ▶️ Como Rodar

**Passo 1:** Subir o Banco de Dados

O projeto possui um `compose.yml` configurado com Postgres e PgAdmin.
````bash
docker compose up -d
````
- Postgres: Porta `5432` (User: `postgres`, Pass: `password`, DB: `user_management`)
- PgAdmin: Acesso em `http://localhost:8081` (Email: `admin@admin.com`, Pass: `password`)

**Passo 2:** Executar a Aplicação

Com o banco rodando e as variáveis configuradas, inicie a aplicação Spring Boot:

**Via Gradle (Linux/Mac):**
````bash
./gradlew bootRun
````

**Via Gradle (Windows):**
````bash
./gradlew.bat bootRun
````

**Passo 3:** Verificar o Funcionamento

1. Ao iniciar, o serviço `ExecuteService` irá popular o banco automaticamente com usuários fictícios ("John Doe", "Jane Smith") se eles não existirem. 
2. O **Scheduler** irá disparar o job conforme o tempo configurado em `BACKUP_CRON`. 
3. Verifique os logs da aplicação:
````bash
Iniciando o Job de Backup...
Initiating upload of file backup_users.csv to S3 bucket...
File uploaded successfully to S3: s3://seu-bucket/backups/2026/05/20/1716..._backup_users.csv
````
4. Verifique o arquivo no seu console da AWS S3.

---
## 📂 Estrutura do Backup no S3

O serviço organiza os arquivos automaticamente por data para facilitar a localização:
````plaintext
backups/
├── 2026/
│   ├── 02/
│   │   ├── 05/
│   │   │   └── 1707158400000_backup_users.csv
````

---
## 🧠 Como Funciona (Under the Hood)
1. **Agendamento**: O `BackupScheduler` acorda conforme a expressão Cron. 
2. **Batch Job:** Inicia o Job `exportUserJob` do Spring Batch. 
3. **Step 1 (Leitura/Escrita):** `JpaPagingItemReader` lê os usuários do banco em páginas de 1000 registros (evita OutOfMemory). 
   - `FlatFileItemWriter` escreve esses registros no arquivo local `backup_users.csv`.
4. **Step 2 (Upload):**
   - O `S3Service` usa o AWS SDK v2 para fazer o upload assíncrono/sincronizado do arquivo gerado para o bucket configurado.