# Roteiro de Deploy — WachaFit no Oracle Cloud Free Tier

Este guia cobre do zero ao sistema no ar, com HTTPS, usando apenas recursos **gratuitos e permanentes** da Oracle Cloud.

---

## Pré-requisitos

- Cartão de crédito ou débito internacional (usado para verificação; **não será cobrado**)
- Um domínio (opcional, mas recomendado para HTTPS)
- Conta no GitHub com o repositório do WachaFit

---

## Parte 1 — Criar conta na Oracle Cloud

1. Acesse [cloud.oracle.com](https://cloud.oracle.com) e clique em **Start for free**.
2. Preencha nome, e-mail, país e crie uma senha.
3. Informe o número do cartão quando solicitado (apenas verificação, sem cobrança).
4. Aguarde o e-mail de confirmação e conclua a ativação da conta.

> A Oracle pode levar até 24h para ativar a conta em alguns casos.

---

## Parte 2 — Criar a máquina virtual (VM)

### 2.1 Acessar o painel

1. Faça login em [cloud.oracle.com](https://cloud.oracle.com).
2. No menu hambúrguer (≡) vá em **Compute → Instances**.
3. Clique em **Create Instance**.

### 2.2 Configurar a instância

| Campo | Valor |
|-------|-------|
| **Name** | `wachafit-prod` |
| **Image** | Ubuntu 22.04 (clique em *Change Image* para selecionar) |
| **Shape** | **VM.Standard.A1.Flex** (ARM — Always Free) |
| **OCPUs** | 4 |
| **Memory** | 24 GB |
| **Network** | VCN padrão (criar automaticamente se não existir) |
| **Public IP** | Assign a public IPv4 address ✅ |

### 2.3 Chave SSH

1. Selecione **Generate a key pair for me**.
2. Clique em **Save private key** — salve o arquivo `.key` em local seguro (ex: `~/.ssh/oracle_wachafit.key`).
3. Clique em **Create**.

### 2.4 Aguardar a VM subir

Aguarde o status mudar para 🟢 **Running** (geralmente 2–3 minutos).  
Anote o **IP público** exibido na página da instância.

---

## Parte 3 — Configurar o firewall da Oracle

Por padrão a Oracle bloqueia tudo exceto SSH. Precisamos abrir as portas 80 (HTTP) e 443 (HTTPS).

1. Na página da instância clique na VCN vinculada.
2. Vá em **Security Lists → Default Security List**.
3. Clique em **Add Ingress Rules** e adicione:

| Source CIDR | Protocol | Port | Descrição |
|-------------|----------|------|-----------|
| `0.0.0.0/0` | TCP | `80` | HTTP |
| `0.0.0.0/0` | TCP | `443` | HTTPS |

4. Clique em **Add Ingress Rules** para salvar.

---

## Parte 4 — Conectar à VM via SSH

No seu terminal local:

```bash
# Ajustar permissão da chave
chmod 400 ~/.ssh/oracle_wachafit.key

# Conectar (substitua pelo IP público da sua VM)
ssh -i ~/.ssh/oracle_wachafit.key ubuntu@<IP_PUBLICO>
```

---

## Parte 5 — Preparar o servidor

Execute os comandos abaixo já dentro da VM:

### 5.1 Atualizar o sistema

```bash
sudo apt update && sudo apt upgrade -y
```

### 5.2 Abrir as portas no firewall do Ubuntu (iptables)

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

### 5.3 Instalar Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker
```

### 5.4 Instalar Git

```bash
sudo apt install -y git
```

---

## Parte 6 — Clonar o projeto

```bash
cd ~
git clone https://github.com/mwacha/wachafit.git
cd wachafit
```

---

## Parte 7 — Configurar as variáveis de ambiente

```bash
cp .env.example .env
nano .env
```

Preencha o arquivo com os valores reais:

```env
# Banco de dados
DB_NAME=wachafit
DB_USER=wachafit
DB_PASSWORD=SenhaForteAqui123!

# JWT — gere uma string aleatória de pelo menos 32 caracteres
JWT_SECRET=coloque-aqui-uma-string-aleatoria-de-32-chars-minimo
JWT_EXPIRATION=3600

# Email SMTP (use SendGrid, Brevo ou Gmail)
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USER=apikey
MAIL_PASSWORD=SG.sua-chave-sendgrid

# URL pública do frontend (com seu domínio ou IP)
APP_FRONTEND_URL=https://seudominio.com

# Porta do nginx
PORT=80

# Pagamento
PAYMENT_GATEWAY=manual
PAYMENT_SUSPEND_AFTER_DAYS=5
```

> **Dica para gerar JWT_SECRET:**
> ```bash
> openssl rand -hex 32
> ```

Salve com `Ctrl+O`, `Enter`, `Ctrl+X`.

---

## Parte 8 — Subir os containers

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

O build do backend pode levar 3–5 minutos na primeira vez (download do Maven + compilação do Java).

### Verificar se tudo subiu

```bash
docker compose -f docker-compose.prod.yml ps
```

Todos os serviços devem aparecer como `running`:

```
NAME        STATUS
db          running
backend     running
frontend    running
```

### Verificar os logs

```bash
# Todos os serviços
docker compose -f docker-compose.prod.yml logs -f

# Só o backend
docker compose -f docker-compose.prod.yml logs -f backend
```

---

## Parte 9 — Testar o acesso

Abra no navegador:

```
http://<IP_PUBLICO>
```

O sistema deve abrir a tela de login do WachaFit. ✅

---

## Parte 10 — Configurar HTTPS com domínio (recomendado)

### 10.1 Apontar o domínio para o IP da VM

No painel do seu registrador de domínio, crie um registro **A**:

| Tipo | Nome | Valor |
|------|------|-------|
| A | `@` ou `app` | `<IP_PUBLICO>` |

Aguarde a propagação do DNS (5–30 minutos).

### 10.2 Instalar Caddy (gerencia HTTPS automaticamente)

```bash
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update && sudo apt install caddy -y
```

### 10.3 Configurar o Caddyfile

```bash
sudo nano /etc/caddy/Caddyfile
```

Substitua o conteúdo por:

```
seudominio.com {
    reverse_proxy localhost:80
}
```

Salve com `Ctrl+O`, `Enter`, `Ctrl+X`.

### 10.4 Reiniciar o Caddy

```bash
sudo systemctl reload caddy
```

O Caddy obtém o certificado SSL automaticamente. Acesse `https://seudominio.com` — deve abrir com cadeado verde. ✅

### 10.5 Atualizar a variável de ambiente

```bash
nano .env
# Alterar:
APP_FRONTEND_URL=https://seudominio.com

# Reiniciar o backend para aplicar
docker compose -f docker-compose.prod.yml restart backend
```

---

## Parte 11 — Comandos úteis do dia a dia

### Atualizar o sistema após novo push

```bash
cd ~/wachafit
git pull origin main
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

### Parar tudo

```bash
docker compose -f docker-compose.prod.yml down
```

### Ver uso de recursos

```bash
docker stats
```

### Backup do banco de dados

```bash
docker exec wachafit-db-1 pg_dump -U wachafit wachafit > backup_$(date +%Y%m%d).sql
```

### Restaurar backup

```bash
cat backup_20260101.sql | docker exec -i wachafit-db-1 psql -U wachafit wachafit
```

---

## Resumo dos serviços

| Serviço | URL |
|---------|-----|
| Sistema | `https://seudominio.com` |
| API | `https://seudominio.com/api/` |
| Swagger | `https://seudominio.com/swagger-ui.html` |

---

## Solução de problemas comuns

**Backend não sobe (erro de banco)**  
→ Verifique as variáveis `DB_USER` e `DB_PASSWORD` no `.env`. Execute `docker compose logs db` para ver o erro.

**Site não abre no navegador**  
→ Confirme que as regras de ingress da Oracle foram salvas (Parte 3) e que o iptables foi configurado (Parte 5.2).

**Erro de certificado HTTPS**  
→ Aguarde a propagação do DNS e verifique com `dig seudominio.com`. O Caddy só emite o certificado quando o domínio já resolve para o IP da VM.

**Porta 80 em uso**  
→ Se o Caddy estiver na porta 80, altere `PORT=8080` no `.env` e aponte o Caddy para `localhost:8080`.
