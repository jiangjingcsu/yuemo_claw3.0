# How to Get a Baidu AI Search API Key

## Step 1: Register on Baidu AI Cloud

1. Visit [Baidu AI Cloud (Qianfan) Console](https://qianfan.baidubce.com/)
2. Sign up or log in with your Baidu account

## Step 2: Enable AI Search API

1. Navigate to the AI Search service page
2. Click "Enable" or "Apply for Access"
3. Review and agree to the terms of service

## Step 3: Get Your API Key

1. Go to the API Keys management page in the console
2. Click "Create New Key"
3. Copy and save the generated API key securely

## Step 4: Configure the Key

Set the environment variable:

```bash
export BAIDU_API_KEY="your-api-key-here"
```

Or add it to your shell profile (e.g., `~/.bashrc`, `~/.zshrc`):

```bash
echo 'export BAIDU_API_KEY="your-api-key-here"' >> ~/.bashrc
source ~/.bashrc
```

## Notes

- The API key is tied to your Baidu AI Cloud account
- Keep your API key secure and do not share it publicly
- Monitor your usage to avoid unexpected charges
- Free tier may have rate limits
