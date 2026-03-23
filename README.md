# 🗺️ Mapa — Achados e Perdidos

Aplicativo Android para registro e compartilhamento de locais de **objetos perdidos e encontrados**, com mapa interativo, chat em tempo real e notificações push.

---

## 📱 Telas

| Autenticação | Mapa (Home) | Salvos | Chat | Perfil |
|:---:|:---:|:---:|:---:|:---:|
| Login / Cadastro | Marcadores no mapa | Seus registros | Mensagens em tempo real | Avaliações e dados |

<p align="center">
  <img width="250" alt="Screenshot_20260322_210659" src="https://github.com/user-attachments/assets/a7c7429f-f15d-4916-8a06-8a01598b5637" />
  <img width="250" alt="Screenshot_20260322_210754" src="https://github.com/user-attachments/assets/8992975e-ec30-4701-b2db-1d635ef37e1c" />
  <img width="250" alt="Screenshot_20260322_210824" src="https://github.com/user-attachments/assets/7ab50c2b-18a9-4b1d-803a-db846c8cd6d5" />

</p>

<p align="center">
  <img width="250" alt="Screenshot_20260322_210855" src="https://github.com/user-attachments/assets/e603511c-4f0e-4c11-bcf5-f84496ff708c" />
  <img width="250" alt="Screenshot_20260322_211349" src="https://github.com/user-attachments/assets/ef426694-d82f-4caa-b63b-56389ae0b23a" />
  <img width="250" alt="Screenshot_20260322_211407" src="https://github.com/user-attachments/assets/9bcd11f4-01fc-4cf9-98b4-aa75fbb3b725" />
</p>

<p align="center">
  <img width="250" alt="Screenshot_20260322_211519" src="https://github.com/user-attachments/assets/f79cd911-a20b-4538-bc71-710a2bd2a233" />
  <img width="250" alt="Screenshot_20260322_211537" src="https://github.com/user-attachments/assets/fd009c9e-205d-45a7-99b8-b783d3b10552" />
  <img width="250" alt="Screenshot_20260322_211621" src="https://github.com/user-attachments/assets/5ce7184a-6621-4452-8ff6-0f0550d448c4" />
</p>

---

## ✨ Funcionalidades

- **Mapa interativo** com Google Maps — visualize todos os registros com marcadores e raio de área
- **Registrar local** como *Perdido* ou *Encontrado*, com fotos, descrição e coordenadas
- **Busca e filtros** de locais diretamente no mapa
- **Chat em tempo real** entre usuários interessados no mesmo registro
- **Notificações push** via FCM para novas mensagens, com suporte a imagem em BigPicture
- **Tela de Salvos** listando somente seus próprios registros
- **Perfil de usuário** com foto, avaliação média por estrelas e contador de avaliações
- **Suporte offline-first** — dados persistidos localmente com Room e sincronizados com o Firestore

---

## 🏗️ Arquitetura

O projeto segue **MVVM + Clean Architecture** com separação clara em camadas:

```
app/
├── di/                         # Injeção de dependências (Koin)
├── model/                      # UiStates e modelos da camada de apresentação
├── ui/
│   ├── component/              # Componentes reutilizáveis (Composables)
│   ├── navigation/             # Navegação (Rotas tipadas com Parcelable)
│   ├── screen/                 # Telas do aplicativo
│   └── theme/                  # Tema (Color, Type, Theme)
├── viewmodels/                 # ViewModels (AuthViewModel, LocationViewModel, ChatViewModel, ChatListViewModel)
├── data/
│   ├── local/
│   │   ├── dao/                # DAOs do Room
│   │   ├── entity/             # Entidades do Room
│   │   └── AppDatabase.kt
│   ├── remote/
│   │   ├── datasource/         # Interfaces das fontes remotas
│   │   ├── dto/                # Data Transfer Objects (Firebase)
│   │   └── firebase/           # Implementações Firebase
│   ├── repository/             # Repositórios (offline-first)
│   └── mapper/                 # Mapeadores entre camadas (DTO ↔ Entity)
├── service/                    # AppFirebaseMessagingService (FCM)
├── util/                       # Extensions e utilitários
└── MapaApplication.kt          # Inicialização do Koin
```

### Fluxo de dados (offline-first)

```
Firebase Firestore ──► LocationRemote ──► LocationRepository ──► LocationDao (Room)
                                                  │
                                                  ▼
                                          LocationViewModel
                                                  │
                                                  ▼
                                          HomeScreen (Compose)
```

O repositório emite primeiro os dados locais (Room) e em paralelo sincroniza com o Firestore, garantindo que a UI responda imediatamente mesmo sem conexão.

---

## 🧩 Stack Tecnológica

| Categoria | Tecnologia |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Linguagem | Kotlin |
| Injeção de Dependências | Koin |
| Banco de dados local | Room |
| Backend / Banco remoto | Firebase Firestore |
| Autenticação | Firebase Authentication |
| Armazenamento de mídia | Firebase Storage |
| Notificações push | Firebase Cloud Messaging (FCM) |
| Analytics | Firebase Analytics |
| Mapa | Google Maps SDK (Maps Compose) |
| Animações | Lottie |
| Navegação | Navigation Compose (rotas tipadas com `sealed interface`) |
| Gerenciamento de estado | `StateFlow` + `collectAsStateWithLifecycle` |

---

## 📂 Principais componentes

### ViewModels

| ViewModel | Responsabilidade |
|---|---|
| `AuthViewModel` | Login, cadastro e estado de autenticação |
| `LocationViewModel` | CRUD de locais (inserir, editar, remover, listar) |
| `ChatViewModel` | Mensagens em tempo real de um chat específico |
| `ChatListViewModel` | Lista de todas as conversas do usuário |

### Telas (`ui/screen`)

| Tela | Descrição |
|---|---|
| `SignInScreen` | Login com e-mail e senha |
| `SignUpScreen` | Cadastro de novo usuário |
| `HomeScreen` | Mapa com marcadores e `BottomSheet` para criar/visualizar locais |
| `SavedScreen` | Listagem dos locais registrados pelo usuário logado |
| `ChatListScreen` | Lista de conversas ativas |
| `ChatScreen` | Conversa em tempo real com suporte a imagens |
| `ProfileScreen` | Perfil do usuário com avaliações por estrelas |

### Componentes reutilizáveis (`ui/components`)

`Header`, `SearchBar`, `BubbleMsg`, `RatingBar`, `CarouselImg`, `AvatarImg`, `LocationDetails`, `LocationForm`, `LoadingOverlay`, `LottieAnimation`, `ConfirmDialog`, `ReviewDialog`, `ImgDialog`, `EditDialog`, `DeleteDialog`

### Modelos de domínio

- **`LocationDTO`** — local com `id`, `uid`, `name`, `type` (Perdido/Encontrado), `description`, coordenadas, `radius` e `imgUrls`
- **`UserDTO`** — usuário com `uid`, `name`, `email`, `photo`, `averageRating`, `ratingCount` e `fcmToken`
- **`MsgDTO`** — mensagem com `id`, `text`, `uid`, `read`, `edited`, `imgUrls` e `timestamp`
- **`ChatDTO`** — conversa vinculando dois usuários a um registro de local
- **`TypeLocation`** — enum `LOST` / `FOUND`

---

## 🔔 Notificações Push (FCM)

O `AppFirebaseMessagingService` recebe mensagens do FCM e exibe notificações com:

- Título e corpo do payload de dados
- Imagem em estilo `BigPicture` (quando disponível)
- Deep link ao clicar: abre o chat correspondente passando `contactUid` e `locationId`
- **Agrupamento** de notificações por contato (Android 7.0+)

---

## 🚀 Como executar

### Pré-requisitos

- Android Studio Hedgehog ou superior
- JDK 17+
- Conta no [Firebase Console](https://console.firebase.google.com/)
- Chave de API do Google Maps

### Configuração

1. **Clone o repositório**
   ```bash
   git clone https://github.com/seu-usuario/mapa.git
   cd mapa
   ```

2. **Configure o Firebase**
   - Crie um projeto no Firebase Console
   - Ative **Authentication** (e-mail/senha), **Firestore**, **Storage** e **Cloud Messaging**
   - Baixe o arquivo `google-services.json` e coloque em `app/`

3. **Configure o Google Maps**
   - Obtenha uma chave de API no [Google Cloud Console](https://console.cloud.google.com/)
   - Adicione no `local.properties` ou diretamente no `AndroidManifest.xml`:
     ```xml
     <meta-data
         android:name="com.google.android.geo.API_KEY"
         android:value="SUA_CHAVE_AQUI" />
     ```

4. **Execute o projeto**
   ```bash
   ./gradlew assembleDebug
   ```
   Ou abra no Android Studio e clique em **Run ▶**.

---

## 📋 Permissões necessárias

| Permissão | Motivo |
|---|---|
| `ACCESS_FINE_LOCATION` | Obter localização precisa do usuário no mapa |
| `ACCESS_COARSE_LOCATION` | Fallback de localização aproximada |
| `READ_MEDIA_IMAGES` | Seleção de fotos da galeria |
| `POST_NOTIFICATIONS` | Exibir notificações push (Android 13+) |
| `INTERNET` | Comunicação com Firebase e Google Maps |

---

## 🗄️ Estrutura do Firestore

```
users/{uid}
  ├── name, email, photo
  ├── averageRating, ratingCount
  ├── reviewerUids[]
  └── fcmToken

locations/{id}
  ├── uid, name, type, description
  ├── latitude, longitude, radius
  ├── date
  └── imgUrls[]

chats/{chatId}
  ├── uid1, uid2, localId
  └── messages/{msgId}
        ├── text, uid, read, edited
        ├── imgUrls[]
        └── timestamp
```

---

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch: `git checkout -b feature/minha-feature`
3. Commit suas alterações: `git commit -m 'feat: adiciona minha feature'`
4. Push: `git push origin feature/minha-feature`
5. Abra um Pull Request

---

## 📄 Licença

Distribuído sob a licença MIT. Consulte o arquivo `LICENSE` para mais detalhes.
