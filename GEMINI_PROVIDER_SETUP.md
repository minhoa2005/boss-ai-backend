# Gemini AI Provider Setup Guide

## Overview

The Gemini AI provider has been successfully integrated into the Boss AI system, providing an additional AI content generation option alongside OpenAI. The system now supports transparent provider selection and automatic failover between multiple AI providers.

## Configuration

### Environment Variables

Add the following environment variables to enable Gemini integration:

```bash
# Gemini API Configuration
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
```

### Application Properties

The following properties are automatically configured in `application.yml`:

```yaml
spring:
  ai:
    gemini:
      api-key: ${GEMINI_API_KEY:}
      api-url: ${GEMINI_API_URL:https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent}
```

### Database Configuration

The system automatically creates the necessary database configuration on startup through `AIProviderDataInitializer`. The Gemini configuration is stored in the `n8n_config` table with:

- `agent_name`: "gemini"
- `agent_url`: Gemini API endpoint
- `x_api_key`: Your Gemini API key
- `model`: "gemini-pro"
- `temperature`: 0.7

## Features

### Provider Capabilities

The Gemini provider supports:

- **Content Types**: Blog, Article, Social Media, Email, Newsletter, Product Descriptions, Advertisements
- **Languages**: Vietnamese, English
- **Tones**: Professional, Friendly, Enthusiastic, Humorous, Authoritative, Casual
- **Max Tokens**: 8,192 tokens per request
- **Rate Limit**: 60 requests per minute
- **Image Analysis**: Yes (unique to Gemini)
- **Image Generation**: No
- **Streaming**: No
- **Function Calling**: Yes

### Cost Optimization

- **Cost per Token**: $0.0002 (lower than OpenAI)
- **Automatic Selection**: System automatically selects the most cost-effective provider
- **Quality Score Range**: 3.5 - 9.5

### Health Monitoring

The Gemini provider includes comprehensive health monitoring:

- **Health Levels**: Healthy, Degraded, Unhealthy, Down
- **Circuit Breaker**: Automatic failover after 5 consecutive failures
- **Health Checks**: Every 30 seconds
- **Metrics Tracking**: Success rate, response time, quality scores

## API Endpoints

### Get Provider Status

```http
GET /api/v1/ai/providers/status
```

Returns status of all providers including Gemini.

### Get Provider Metrics

```http
GET /api/v1/ai/providers/metrics/Gemini
```

Returns detailed metrics for the Gemini provider.

### Force Health Check

```http
POST /api/v1/ai/providers/health/check/Gemini
```

Forces a health check for the Gemini provider.

## Usage

### Transparent Provider Selection

The system automatically selects the optimal provider based on:

- **Cost** (40% weight): Lower cost providers preferred
- **Availability** (30% weight): Higher success rate preferred  
- **Quality** (20% weight): Higher quality scores preferred
- **Response Time** (10% weight): Faster providers preferred

### Automatic Failover

If the primary provider fails:

1. System automatically tries the next best provider
2. Circuit breaker prevents repeated failures
3. Exponential backoff between retry attempts
4. Maximum 3 retry attempts with different providers

### Content Generation

Content generation requests are handled transparently:

```java
// The system automatically selects between OpenAI and Gemini
ContentGenerateResponse response = aiProviderManager.generateContent(request, user);
```

## Monitoring

### Health Status

Monitor provider health through:

- **Dashboard**: Real-time health status display
- **Logs**: Detailed health check results
- **Metrics**: Success rates, response times, error rates
- **Alerts**: Automatic notifications for provider issues

### Performance Metrics

Track provider performance:

- **Success Rate**: Percentage of successful requests
- **Average Response Time**: Mean response time in milliseconds
- **Quality Score**: Average content quality rating
- **Cost Efficiency**: Cost per successful generation

## Troubleshooting

### Common Issues

1. **Provider Not Available**
   - Check API key configuration
   - Verify network connectivity
   - Review rate limiting

2. **High Error Rate**
   - Check API quota limits
   - Review request format
   - Verify model availability

3. **Slow Response Times**
   - Check network latency
   - Review request complexity
   - Consider load balancing

### Debug Commands

```bash
# Check provider status
curl -X GET http://localhost:8080/api/v1/ai/providers/status

# Force health check
curl -X POST http://localhost:8080/api/v1/ai/providers/health/check/Gemini

# Get detailed metrics
curl -X GET http://localhost:8080/api/v1/ai/providers/metrics/Gemini
```

## Testing

### Unit Tests

Run Gemini provider tests:

```bash
./gradlew test --tests "*GeminiProviderTest*"
```

### Manual Testing

1. Set environment variables
2. Start the application
3. Check provider registration in logs
4. Test content generation through API
5. Monitor health status

## Implementation Details

### Key Components

- **GeminiProvider**: Main provider implementation
- **AIProviderManager**: Provider selection and failover
- **AIProviderHealthMonitor**: Health monitoring service
- **AIProviderMetricsService**: Performance metrics tracking
- **AIProviderDataInitializer**: Database configuration setup

### Request Flow

1. User submits content generation request
2. AIProviderManager selects optimal provider
3. GeminiProvider processes request
4. Response returned with metrics tracking
5. Health status updated

### Error Handling

- **Validation Errors**: Input validation with clear error messages
- **API Errors**: Proper error mapping from Gemini API
- **Network Errors**: Retry logic with exponential backoff
- **Circuit Breaker**: Automatic provider switching on failures

## Security

### API Key Management

- Environment variable configuration
- Secure storage in database
- No logging of sensitive data
- Proper error message sanitization

### Rate Limiting

- Built-in rate limiting (60 RPM)
- Circuit breaker protection
- Load balancing across providers
- Automatic backoff on limits

## Future Enhancements

### Planned Features

- **Streaming Support**: Real-time content generation
- **Custom Models**: Support for fine-tuned models
- **Batch Processing**: Multiple requests in single API call
- **Advanced Analytics**: Detailed usage analytics

### Configuration Options

- **Custom Endpoints**: Support for custom Gemini endpoints
- **Model Selection**: Dynamic model selection based on content type
- **Temperature Tuning**: Per-request temperature configuration
- **Token Optimization**: Dynamic token limit adjustment

## Support

For issues or questions:

1. Check application logs for detailed error messages
2. Review provider health status in dashboard
3. Verify API key and configuration
4. Test with simple requests first
5. Monitor metrics for performance insights

The Gemini provider integration is now complete and ready for production use with full monitoring, failover, and optimization capabilities.