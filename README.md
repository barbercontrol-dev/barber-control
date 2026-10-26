# BarberControl

Sistema de gerenciamento para barbearias.

## Cadastro e operação

- O cadastro inicial cria a conta da barbearia com nome do estabelecimento, responsável, email e senha. Somente o responsável possui acesso ao sistema.
- Depois de entrar, use **Barbeiros** para cadastrar profissionais com nome, telefone e CPF (eles não recebem login). Os serviços e agendamentos são associados à barbearia.
- Em **Agendamentos**, selecione um barbeiro para consultar ou criar horários. A opção **Todas as agendas** permite consultar todos os agendamentos da barbearia.
- O agendamento público lista os barbeiros cadastrados e apresenta os serviços da barbearia do profissional escolhido.

## API relevante

- `POST /auth/registrar`: recebe `nomeBarbearia`, `nomeResponsavel`, `email`, `senha` e `consentimentoLgpd`.
- `GET /api/barbeiros` e `POST /api/barbeiros`: listam e cadastram barbeiros da barbearia autenticada.
- `GET /api/agendamentos?barbeiroId={id}`: lista os agendamentos do barbeiro escolhido; sem o parâmetro lista a barbearia inteira.
- Os serviços criados após esta atualização ficam associados à barbearia autenticada. Serviços antigos sem `barbearia_id` precisam ser atribuídos à barbearia correta antes de voltarem a aparecer; não são compartilhados automaticamente entre contas.
- Para migrar serviços antigos, identifique o ID do responsável em `usuario` e associe os serviços legados correspondentes em `servico` preenchendo `barbearia_id`.

## Preparar deploy

O frontend é estático e a API usa Java 21, Spring Boot e MySQL. A API pode ser publicada por Docker; o arquivo `Dockerfile` está na raiz do repositório.

Para executar localmente, defina `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` e `JWT_SECRET` no ambiente antes de iniciar a API. Nenhuma senha de banco ou chave JWT deve ser adicionada ao repositório.

### API no Railway

Crie um serviço MySQL e um serviço para a API usando o repositório. Selecione o builder Docker e mantenha o contexto na raiz. Configure estas variáveis no serviço da API:

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL`: JDBC URL do MySQL, no formato `jdbc:mysql://HOST:PORT/DB`
- `SPRING_DATASOURCE_USERNAME`: usuário do MySQL
- `SPRING_DATASOURCE_PASSWORD`: senha do MySQL
- `JWT_SECRET`: segredo aleatório forte de pelo menos 32 bytes; não reutilize o valor de desenvolvimento
- `CORS_ALLOWED_ORIGINS`: origem completa do frontend, por exemplo `https://seu-site.pages.dev`, sem barra no final

O perfil `prod` desativa o log SQL, usa a porta fornecida pela plataforma e exige as variáveis de conexão e segredo. O Hibernate cria/atualiza as tabelas no primeiro deploy (`ddl-auto=update`), mas não copia os registros do MySQL local; exporte e restaure os dados que precisar preservar. Faça backup antes de implantar mudanças futuras de modelo. Configure o volume e os backups do MySQL conforme o plano do provedor.

### Frontend no Cloudflare Pages

Conecte o mesmo repositório no Cloudflare Pages, deixe o comando de build vazio e configure `Frontend` como diretório de saída. Depois que a API receber uma URL pública, atualize `Frontend/js/config.js` para usar essa URL HTTPS, sem barra no final. Use a origem publicada exatamente igual em `CORS_ALLOWED_ORIGINS` e publique o frontend novamente.

### Checagem antes de abrir ao público

Teste o cadastro e login, operações autenticadas, agendamento público, carregamento de serviços e barbeiros e persistência após reiniciar a API. Confirme também HTTPS nos dois serviços e um backup restaurável do banco. O repositório não publica automaticamente até que esses serviços e variáveis sejam configurados nas respectivas contas.
