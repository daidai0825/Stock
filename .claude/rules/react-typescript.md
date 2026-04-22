# Rule: React + TypeScript 開發規範

> **適用範圍**：`*.ts`、`*.tsx`
> **適用 Agents**：Felix、Fiona、Preston、Linus
> **技術版本**：React 18+、TypeScript 5.x、Vite 5.x、Ant Design 5.x

---

## 版本要求

| 技術 | 版本 |
|------|------|
| React | 18+ |
| TypeScript | 5.x |
| Vite | 5.x |
| Ant Design | 5.x |
| Node.js | 20+（LTS） |
| 套件管理 | npm 10+ 或 pnpm 8+ |

## TypeScript 設定

```json
// tsconfig.json
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "noImplicitAny": true,
    "exactOptionalPropertyTypes": true,
    "noFallthroughCasesInSwitch": true,
    "noImplicitReturns": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "isolatedModules": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  }
}
```

## 目錄結構

```
src/
├── components/
│   ├── common/         # 通用元件（封裝 antd）
│   └── business/       # 業務元件
├── pages/              # 路由頁面
│   └── UserManagement/
│       ├── index.tsx
│       ├── components/
│       └── hooks/
├── hooks/              # 共用 hooks
├── services/           # API 呼叫層
├── stores/             # 全域狀態（Zustand / Redux）
├── types/              # 共用型別
├── utils/              # 工具函式
├── constants/          # 常數
├── routes/             # 路由配置
└── App.tsx
```

## 元件規範

### 命名

- 檔名：PascalCase（`UserProfile.tsx`）
- 元件：PascalCase
- Hook：camelCase + `use` 前綴（`useUserData`）
- 工具函式：camelCase
- 常數：UPPER_SNAKE_CASE
- 型別：PascalCase

### 元件結構

```typescript
import { type FC, useState, useEffect, useMemo } from 'react';
import { Card, Button } from 'antd';
import { type User } from '@/types';

interface UserProfileProps {
  userId: string;
  onEdit?: (userId: string) => void;
}

export const UserProfile: FC<UserProfileProps> = ({ userId, onEdit }) => {
  // 1. Hooks
  const [loading, setLoading] = useState(false);
  const { data: user } = useUser(userId);

  // 2. 衍生狀態
  const displayName = useMemo(() => user?.name ?? '匿名', [user]);

  // 3. Effects
  useEffect(() => {
    // ...
  }, [userId]);

  // 4. Event handlers
  const handleEdit = () => {
    onEdit?.(userId);
  };

  // 5. Render
  return (
    <Card title={displayName}>
      <Button onClick={handleEdit}>編輯</Button>
    </Card>
  );
};
```

### 元件原則

- 單一職責（一個元件做一件事）
- Props 用 interface（不用 type，除非聯合型別）
- 預設使用 Functional Component + Hooks
- 避免 prop drilling 超過 3 層（用 Context 或 Zustand）

## 型別系統

### 禁用 any

```typescript
// ❌ 禁止
function process(data: any) { }

// ✅ 用 unknown 配合 type guard
function process(data: unknown) {
  if (typeof data === 'string') { /* ... */ }
}

// ✅ 或明確型別
function process(data: User) { }
```

### Type vs Interface

```typescript
// 物件結構：用 interface（可擴展）
interface User {
  id: string;
  name: string;
}

// 聯合型別、工具型別：用 type
type Status = 'active' | 'inactive';
type UserKeys = keyof User;
```

### 嚴格選填

```typescript
// 啟用 exactOptionalPropertyTypes 後
interface Props {
  name?: string;  // 只能是 string 或 undefined，不能是 null
}
```

## Hooks 使用準則

### 依賴陣列

```typescript
// ✅ 完整依賴
useEffect(() => {
  fetchUser(userId);
}, [userId]);

// ❌ 缺漏依賴（stale closure）
useEffect(() => {
  fetchUser(userId);
}, []);  // ESLint exhaustive-deps 會警告
```

### useMemo / useCallback

只在以下情境使用：
- 計算昂貴
- 傳給 memo 元件作 props
- 作為其他 hook 的依賴

### 自訂 Hook

```typescript
// hooks/useUser.ts
import { useQuery } from '@tanstack/react-query';
import { userService } from '@/services/userService';

export const useUser = (userId: string) => {
  return useQuery({
    queryKey: ['user', userId],
    queryFn: () => userService.getUser(userId),
    enabled: !!userId,
  });
};
```

## API 呼叫

### Services 層

```typescript
// services/api.ts
import axios, { type AxiosInstance } from 'axios';

export const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 攔截器：注入 token
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 攔截器：統一處理 Envelope
api.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data;
    if (code !== 0) {
      throw new BusinessError(code, message);
    }
    return { ...response, data };
  },
  (error) => Promise.reject(error)
);
```

### Service 範例

```typescript
// services/userService.ts
import { api } from './api';
import { type User, type CreateUserRequest } from '@/types';

export const userService = {
  async getUser(userId: string): Promise<User> {
    const { data } = await api.post<User>('/api/v1/user/get', { userId });
    return data;
  },

  async createUser(request: CreateUserRequest): Promise<User> {
    const { data } = await api.post<User>('/api/v1/user/create', request);
    return data;
  },
};
```

## 狀態管理

### 局部狀態：useState
### 跨元件狀態：Context（少量）/ Zustand（中大型）
### 伺服器狀態：React Query / SWR
### 表單狀態：Ant Design Form + Zod

```typescript
// 表單驗證範例
import { z } from 'zod';

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
});

type FormValues = z.infer<typeof schema>;
```

## 環境變數

```typescript
// vite-env.d.ts
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_LOG_LEVEL: 'debug' | 'info' | 'warn' | 'error';
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
```

## 樣式

### 優先順序

1. Ant Design 內建（最優先）
2. Ant Design ConfigProvider 客製化主題
3. CSS Modules（`*.module.css`）
4. 必要時用 styled-components / emotion

避免全域 CSS 污染。

## 可存取性（a11y）

- 互動元素加 `aria-label`
- 圖片加 `alt`
- 表單欄位加 `<label>`
- 鍵盤可操作
- 顏色對比 ≥ WCAG AA（4.5:1）

## 禁止事項

- **禁止**使用 `any`（用 unknown + type guard）
- **禁止**class component（除非整合舊程式碼）
- **禁止**直接修改 state（用 setState）
- **禁止**在元件內寫 API URL（走 services）
- **禁止**忽略 ESLint warnings
- **禁止**hard-code 文字（使用 i18n 或 constants）
- **禁止**使用 `dangerouslySetInnerHTML` 未 sanitize
- **禁止**將 token 存於 localStorage（建議 httpOnly cookie）
- **禁止**降級處理或本地快取
