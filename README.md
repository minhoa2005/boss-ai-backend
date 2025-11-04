# Boss AI Backend

Spring Boot REST API backend for the Boss AI content automation platform.

## Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Java Version**: 17
- **Build Tool**: Gradle
- **Database**: PostgreSQL with Spring Data JPA
- **Caching**: Redis with Spring Data Redis
- **Security**: Spring Security with JWT authentication
- **AI Integration**: OpenAI with dynamic configuration via N8nConfig
- **Monitoring**: Micrometer with Prometheus
- **Email**: Spring Boot Mail Starter

## Project Structure

```
src/main/java/ai/content/auto/
├── annotation/      # Custom annotations
│   └── Loggable.java
├── config/          # Configuration classes
│   ├── DatabaseConfig.java
│   ├── RedisConfig.java
│   └── SecurityConfig.java
├── controller/      # REST API controllers
├── dtos/           # Data Transfer Objects
│   └── auth/       # Authentication DTOs
├── entity/         # JPA entities
│   ├── User.java
│   ├── UserRole.java
│   ├── UserRoleId.java
│   └── ContentVersion.java    # ✅ NEW: Content versioning entity
├── exception/      # Custom exceptions
├── mapper/         # Entity-DTO mappers
├── repository/     # Data access layer
│   ├── UserRepository.java
│   └── UserRoleRepository.java
├── security/       # Security components
│   ├── JwtAuthenticationEntryPoint.java
│   └── JwtAuthenticationFilter.java
├── service/        # Business logic services
│   ├── AuthService.java
│   ├── EmailService.java
│   ├── JwtService.java
│   └── UserService.java
└── utils/          # Utility classes
```

## Key Features ✅ **UPDATED**

### Content Versioning System ✅ **NEW**
- **Version Management**: Complete content versioning with automatic version numbering
- **Performance Tracking**: Quality, readability, and SEO score tracking for each version
- **A/B Testing**: Built-in comparison system for version performance analysis
- **JSONB Storage**: Flexible parameter storage using PostgreSQL JSONB
- **Audit Trail**: Complete audit logging with user tracking and timestamps
- **Business Logic**: Built-in performance calculation and comparison methods

### AI Content Generation
- **Multi-Provider Support**: OpenAI integration with extensible provider architecture
- **Response Logging**: Comprehensive logging of AI interactions for audit and cost tracking
- **Cost Optimization**: Automatic cost calculation and token usage tracking
- **Quality Assessment**: Automated content quality scoring and analysis

### Authentication & Security
- **JWT Authentication**: Stateless authentication with access and refresh tokens
- **Role-Based Access Control**: Comprehensive RBAC with user role management
- **Account Security**: Account locking, password policies, and secure password hashing
- **API Security**: CORS configuration, security headers, and endpoint protection

### Database & Performance
- **Connection Pooling**: HikariCP with optimized connection management
- **Caching**: Redis integration for session management and performance optimization
- **Transaction Management**: Optimized transaction boundaries and isolation levels
- **Performance Monitoring**: Built-in metrics and monitoring capabilities

## Configuration

### Security Configuration
The `SecurityConfig` class handles:
- JWT authentication setup with stateless sessions
- JWT token validation and processing
- API endpoint security rules with role-based access
- CORS configuration with environment-specific origins
- Security headers (HSTS, frame options, content type protection)

### Database Configuration
The `DatabaseConfig` class provides comprehensive database setup with:
- **HikariCP Connection Pool**: High-performance JDBC connection pooling
- **JPA/Hibernate Configuration**: Optimized for PostgreSQL with performance tuning
- **Connection Pool Settings**: Configurable pool size, timeouts, and leak detection
- **Performance Optimizations**: Prepared statement caching, batch processing, and connection reuse
- **Production-Ready**: Includes monitoring and connection lifecycle management

#### Database Features
- **Connection Pooling**: HikariCP with configurable pool sizes (default: 20 max, 5 min idle)
- **Performance Tuning**: Prepared statement caching, batch operations, and connection reuse
- **Monitoring**: Connection leak detection and pool statistics
- **JPA Auditing**: Automatic creation and modification timestamps
- **Transaction Management**: Optimized transaction handling with proper isolation
- **Hibernate Optimizations**: Batch processing, optimized for production performance
- **Production Configuration**: SQL formatting and comments disabled for optimal performance

### Redis Configuration
The `RedisConfig` class handles caching and session management:
- **Connection Pooling**: Lettuce connection factory with Apache Commons Pool2
- **Flexible Timeout Configuration**: Support for multiple timeout formats (ms, s, m)
- **Serialization**: Jackson2 JSON serialization for complex objects
- **Cache Manager**: Redis-based caching with configurable TTL
- **Performance**: Connection pooling and optimized serialization

### Redis Configuration
- Redis connection setup
- Caching strategies
- Session management

## Development

### Prerequisites
- Java 17+
- PostgreSQL 12+
- Redis 6+

### Running the Application

```bash
# Development mode
./gradlew bootRun

# Build application
./gradlew build

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

### Environment Variables

Configure the following environment variables for different environments:

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/boss-ai
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password

# HikariCP Connection Pool (Optional - defaults provided)
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5
SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT=300000
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=20000
SPRING_DATASOURCE_HIKARI_LEAK_DETECTION_THRESHOLD=60000
SPRING_DATASOURCE_HIKARI_MAX_LIFETIME=1800000

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_TIMEOUT=2000ms  # Supports formats: 2000ms, 2s, 2000 (default: ms)

# OpenAI Configuration (managed via N8nConfig table)
# No environment variables needed - configuration stored in database

# JWT Security
JWT_SECRET=your_jwt_secret_key_minimum_256_bits

# Email Service
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM=noreply@yourdomain.com

# CORS Configuration
CORS_ALLOWED_ORIGINS=https://yourdomain.com,http://localhost:3000
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS,PATCH
CORS_ALLOW_CREDENTIALS=true

# Account Security
MAX_FAILED_ATTEMPTS=5
LOCK_DURATION_MINUTES=30

# Frontend Integration
FRONTEND_URL=https://yourdomain.com
```

## API Documentation

The application exposes RESTful APIs for:
- User authentication and authorization
- Configuration management (industry, content types, languages, tones, target audiences)
- AI content generation with OpenAI integration
- OpenAI response logging and analytics
- Data management operations
- Administrative functions

### AI Content Generation

The application includes comprehensive AI content generation capabilities with automatic logging:

#### OpenAI Response Logging
All OpenAI API interactions are automatically logged to the `openai_response_log` table for:
- **Token Usage Tracking**: Calculate costs and manage quotas
- **Performance Monitoring**: Track response times and identify bottlenecks
- **Audit Trail**: Maintain complete history of AI interactions
- **User Analytics**: Analyze usage patterns per user

#### OpenaiResponseLogDto Structure
```java
public record OpenaiResponseLogDto(
    Long id,
    UserDto user,                    // Associated user who made the request
    Map<String, Object> contentInput,    // User's input content and parameters
    Map<String, Object> openaiResult,    // Complete OpenAI API response
    OffsetDateTime createAt,             // Request creation timestamp
    OffsetDateTime responseTime,         // Response received timestamp
    @Size(max = 50) String model         // OpenAI model used (e.g., "gpt-4")
) implements Serializable {}
```

#### Key Features
- **JSON Storage**: Both input and output stored as JSON in PostgreSQL using `@JdbcTypeCode(SqlTypes.JSON)`
- **User Association**: Each log entry linked to the authenticated user via `@ManyToOne` relationship
- **Performance Tracking**: Separate timestamps for request creation and response receipt
- **Model Tracking**: Records which OpenAI model was used for billing and analytics
- **Flexible Data Structure**: JSON columns accommodate varying OpenAI response formats

#### Content Generation Flow
1. User submits content generation request with parameters (industry, content type, tone, etc.)
2. System calls OpenAI API with user's input
3. **Response automatically logged** to `openai_response_log` table
4. Token usage calculated for billing purposes
5. Generated content returned to user
6. Optional: Content can be saved to user's content library

### Configuration Endpoints

The ConfigController provides comprehensive configuration management with three distinct endpoint categories:

#### 1. Primary Configuration Endpoints (Role-Based Access)
These endpoints implement intelligent role-based filtering:
- **ADMIN**: Returns all available configurations from `configs_primary` table
- **USER**: Returns only user's selected configurations from `configs_user` joined with `configs_primary`

- `GET /api/v1/config/industry` - Get industry configurations with role-based access
- `GET /api/v1/config/content_type` - Get content type configurations with role-based access
- `GET /api/v1/config/language` - Get language configurations with role-based access
- `GET /api/v1/config/tone` - Get tone configurations with role-based access
- `GET /api/v1/config/target_audience` - Get target audience configurations with role-based access

#### 2. Available Configuration Endpoints (All Options for Selection UI)
These endpoints return all ConfigsPrimary data regardless of user selection - designed for selection UI and admin purposes:

- `GET /api/v1/config/available/industry` - Get all available industry configurations
- `GET /api/v1/config/available/content_type` - Get all available content type configurations
- `GET /api/v1/config/available/language` - Get all available language configurations
- `GET /api/v1/config/available/tone` - Get all available tone configurations
- `GET /api/v1/config/available/target_audience` - Get all available target audience configurations

#### 3. User Selection Details Endpoints (ConfigsUser Metadata)
These endpoints return ConfigsUserDto objects with selection metadata and role-based access:
- **ADMIN**: Returns all users' selection details
- **USER**: Returns current user's selection details

- `GET /api/v1/config/user/industry` - Get industry selection details with metadata
- `GET /api/v1/config/user/content_type` - Get content type selection details with metadata
- `GET /api/v1/config/user/language` - Get language selection details with metadata
- `GET /api/v1/config/user/tone` - Get tone selection details with metadata
- `GET /api/v1/config/user/target_audience` - Get target audience selection details with metadata

#### Security & Authorization
- **Authentication Required**: All endpoints require valid JWT Bearer token
- **Role-Based Access Control**: Uses `@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")`
- **SecurityUtil Integration**: Automatic role detection with `SecurityUtil.isCurrentUserAdmin()`
- **User Context Management**: Seamless access to current user via `SecurityUtil.getCurrentUserId()`

#### Response Format
- **Primary Endpoints**: Return `ConfigsPrimaryDto` objects with configuration data
- **Available Endpoints**: Return `ConfigsPrimaryDto` objects for all available options
- **User Selection Endpoints**: Return `ConfigsUserDto` objects with selection metadata and embedded `ConfigsPrimaryDto`

API documentation is available via Spring Boot Actuator endpoints when running in development mode.

## Monitoring

The application includes:
- Prometheus metrics endpoint (`/actuator/prometheus`)
- Health check endpoint (`/actuator/health`)
- Application info endpoint (`/actuator/info`)

## Security

The application implements comprehensive security features:

### JWT Authentication
- **JWT Service**: Token generation and validation using JJWT library
- **JWT Authentication Filter**: Processes JWT tokens from Authorization header
- **JWT Authentication Entry Point**: Handles unauthorized access attempts
- **Stateless Authentication**: No server-side session storage

### User Management
- **UserDetailsService**: Custom implementation integrated with User entity
- **Password Encoding**: BCrypt with strength 12 for secure password hashing
- **Account Security**: Failed login attempt tracking and account locking
- **Email Verification**: Account activation via email verification tokens

### Security Configuration
- **Spring Security**: Modern configuration with method-level security
- **CORS**: Configurable cross-origin resource sharing
- **Security Headers**: HSTS, frame options, and content type protection
- **Role-based Access Control**: User roles and permissions system
- **SecurityUtil**: Current user context management and role checking utilities

### API Security
- **Protected Endpoints**: JWT authentication required for most endpoints
- **Public Endpoints**: Authentication, health checks, and API documentation
- **Admin Endpoints**: Restricted to users with ADMIN role
- **Programmatic Authorization**: SecurityUtil methods for role checking in services and controllers

## SecurityUtil - Role Management

The `SecurityUtil` class provides convenient methods for checking user roles and managing current user context in your services and controllers.

### Available Methods

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtil {
    
    // Get current user information
    Long getCurrentUserId()           // Returns current authenticated user's ID
    User getCurrentUser()             // Returns current authenticated user entity
    String getCurrentUsername()       // Returns current authenticated username
    
    // Role checking methods
    boolean isCurrentUserAdmin()      // Check if current user has ADMIN role
    boolean hasRole(String role)      // Check if current user has specific role (without ROLE_ prefix)
}
```

### Usage Examples

#### Service-Level Authorization
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {
    
    private final SecurityUtil securityUtil;
    
    public void deleteContent(Long contentId) {
        // Only admins can delete content
        if (!securityUtil.isCurrentUserAdmin()) {
            throw new ForbiddenException("Admin access required");
        }
        
        // Perform deletion
        log.info("Content {} deleted by admin {}", contentId, securityUtil.getCurrentUsername());
    }
    
    public ContentResponse updateContent(Long contentId, UpdateContentRequest request) {
        Long currentUserId = securityUtil.getCurrentUserId();
        
        // Users can only update their own content unless they're admin
        Content content = contentRepository.findById(contentId)
            .orElseThrow(() -> new NotFoundException("Content not found"));
            
        if (!content.getCreatedBy().equals(currentUserId) && !securityUtil.isCurrentUserAdmin()) {
            throw new ForbiddenException("You can only update your own content");
        }
        
        // Perform update
        return updateContentInTransaction(contentId, request);
    }
}
```

#### Controller-Level Authorization
```java
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final SecurityUtil securityUtil;
    
    @GetMapping("/users")
    public ResponseEntity<BaseResponse<List<UserResponse>>> getAllUsers() {
        // Method-level admin check
        if (!securityUtil.isCurrentUserAdmin()) {
            throw new ForbiddenException("Admin access required");
        }
        
        // Get users logic...
        List<UserResponse> users = userService.getAllUsers();
        
        BaseResponse<List<UserResponse>> response = new BaseResponse<List<UserResponse>>()
            .setErrorMessage("Users retrieved successfully")
            .setData(users);
            
        return ResponseEntity.ok(response);
    }
}
```

#### Role-Based Operations
```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final SecurityUtil securityUtil;
    
    public void assignRole(Long userId, String role) {
        // Check if current user can assign roles
        if (!securityUtil.hasRole("ADMIN") && !securityUtil.hasRole("SUPER_ADMIN")) {
            throw new ForbiddenException("Admin or Super Admin role required");
        }
        
        // Prevent privilege escalation
        if ("SUPER_ADMIN".equals(role) && !securityUtil.hasRole("SUPER_ADMIN")) {
            throw new ForbiddenException("Only Super Admin can assign Super Admin role");
        }
        
        // Assign role logic...
        assignRoleInTransaction(userId, role);
    }
}
```

### Error Handling

When authorization fails, throw appropriate exceptions:

```java
// For insufficient permissions
throw new ForbiddenException("Admin access required");

// For unauthenticated users (handled automatically by SecurityUtil)
// BusinessException("User is not authenticated") - HTTP 401

// For authorization failures (handled by GlobalExceptionHandler)
// ForbiddenException - HTTP 403
```

## Authentication API

### Login Response Format
```json
{
  "errorCode": "SUCCESS",
  "errorMessage": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshExpiresIn": 604800,
    "user": {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "role": "USER",
      "emailVerified": true,
      "profilePictureUrl": "https://example.com/avatar.jpg"
    }
  }
}
```

### Authentication Endpoints
- `POST /api/v1/auth/login` - User login with credentials
- `POST /api/v1/auth/register` - User registration with email verification
- `POST /api/v1/auth/user-active` - Account activation via email token
- `POST /api/v1/auth/refresh` - Token refresh with validation

## Recent Updates

### Frontend UI Enhancements ✅ **LATEST UPDATE**

#### Content Generation Page Redesign ✅ **UPDATED**
The main content generation page has been redesigned with a more professional and compact layout:

**Layout Improvements:**
- **Compact Header Design**: Reduced header size and improved spacing for better screen utilization
- **Enhanced Button Sizing**: Smaller, more proportional action buttons with consistent spacing
- **Improved Grid System**: Better responsive breakpoints including ultra-wide screen support (xl breakpoints)
- **Professional Typography**: Refined text hierarchy with appropriate font sizes and weights
- **Optimized Content Flow**: Better sidebar and main content area proportions

**Visual Enhancements:**
- **Consistent Icon Sizing**: Standardized icon sizes throughout the interface (32px for main icons, 20px for secondary)
- **Better Color Hierarchy**: Improved use of Vuetify's color system for better visual separation
- **Enhanced Cards**: Refined card styling with better padding and visual hierarchy
- **Responsive Design**: Improved adaptation to different screen sizes with proper breakpoint usage

**User Experience Improvements:**
- **Faster Navigation**: More compact header allows more content to be visible
- **Better Focus**: Cleaner design reduces visual clutter and improves user focus
- **Professional Appearance**: More polished interface suitable for business environments
- **Improved Accessibility**: Better contrast ratios and focus indicators
- **Form Field Consistency**: Optimized form field density for uniform appearance and better visual flow ✅ **LATEST**

#### Dashboard Navigation Simplification ✅ **LATEST UPDATE**
The frontend horizontal dashboard navigation has been streamlined for better user experience:

**Changes Made:**
- **Simplified Structure**: Reduced from complex multi-level dashboard menu to single Analytics Dashboard item
- **Removed Admin-Specific Dashboard**: Eliminated separate admin analytics dashboard for cleaner navigation
- **Removed Grouped Dashboard Menu**: Removed nested dashboard menu with multiple dashboard types
- **Focus on Core Analytics**: Maintained only essential Analytics Dashboard for primary user workflow

**Benefits:**
- **Reduced Cognitive Load**: Users see only essential dashboard navigation
- **Faster Navigation**: Direct access to primary analytics without nested menus
- **Cleaner UI**: Less cluttered horizontal navigation bar
- **Better Mobile Experience**: Simplified navigation works better on smaller screens
- **Content-First Approach**: Aligns with application's focus on content generation and management

### OpenAI Service Complete Implementation ✅ **FULL IMPLEMENTATION**
- **Advanced Prompt Engineering**: Implemented structured system and user prompts with role-based AI personas for different content types
- **Content Type Intelligence**: Specialized prompts for blog, social media, email, product descriptions, and advertisements
- **Multi-Language Support**: Natural Vietnamese and English content generation with cultural adaptation
- **Tone Customization**: Professional, friendly, enthusiastic, humorous, authoritative, and casual tones
- **Dynamic Token Management**: Content type-specific max token allocation using ContentConstants for optimal performance
- **Quality Assessment**: Automated content quality scoring and readability analysis with configurable thresholds
- **Cost Optimization**: Centralized cost calculation with ContentConstants.COST_PER_1K_TOKENS for maintainable pricing
- **New Response Format Support**: Updated to handle OpenAI's new 'output' array structure with enhanced parsing capabilities
- **Enhanced Content Extraction**: Type-based content filtering from nested response arrays with robust error handling
- **Status Validation**: Comprehensive status checking at multiple levels (response and output) for reliability
- **Token Usage Updates**: Updated field mapping for 'input_tokens' and 'output_tokens' to match current OpenAI API
- **Error Handling Enhancement**: Improved error detection and logging with detailed context for better debugging
- **Backward Compatibility**: Maintains existing client response format while adapting to new OpenAI structure
- **Content Processing Pipeline**: Complete content cleaning, formatting, and quality metrics calculation
- **Performance Monitoring**: Processing time tracking and cost estimation for operational insights
- **Complete Method Implementation**: Full `processOpenAiResponse()` method with all helper functions ✅ **COMPLETE**
- **Content Quality Analysis**: Comprehensive quality scoring with sentence variety, paragraph structure, and uniqueness metrics ✅ **COMPLETE**
- **Readability Assessment**: Advanced readability scoring using sentence complexity and optimal word-per-sentence ratios ✅ **COMPLETE**
- **Content Cleaning Pipeline**: Automated content formatting with whitespace normalization and sentence spacing ✅ **COMPLETE**
- **Multi-language Text Processing**: Content cleaning supports both Vietnamese and English character sets ✅ **COMPLETE**
- **Centralized Constants Integration**: All thresholds and limits managed through ContentConstants for maintainability ✅ **COMPLETE**

#### Complete Implementation Details ✅ **NEW**
The OpenAiService now includes a fully implemented content processing pipeline:

**Content Processing Features:**
- **Multi-Level Error Handling**: Comprehensive error checking at API, response, and content levels
- **Type-Safe Response Parsing**: Robust parsing of nested OpenAI response structures with proper type checking
- **Content Quality Metrics**: Automated calculation of quality scores, readability scores, and content statistics
- **Advanced Content Cleaning**: Whitespace normalization, sentence spacing, and paragraph formatting
- **Cost Estimation**: Real-time cost calculation using centralized pricing constants
- **Performance Tracking**: Processing time monitoring and detailed logging for optimization
- **Multi-Language Support**: Content cleaning optimized for both Vietnamese and English text patterns
- **Configurable Thresholds**: All quality and readability thresholds managed through ContentConstants

**Helper Methods Implemented:**
- `processOpenAiResponse()`: Complete response processing with error handling and metrics calculation
- `cleanGeneratedContent()`: Advanced content formatting and cleaning
- `calculateQualityScore()`: Comprehensive quality assessment based on multiple factors
- `calculateReadabilityScore()`: Sentence complexity and structure analysis
- `calculateEstimatedCost()`: Cost calculation using centralized constants
- `countWords()`, `countSentences()`, `countParagraphs()`: Content analysis utilities

**Quality Assessment Criteria:**
- **Length Optimization**: Validates content length against optimal ranges using ContentConstants
- **Sentence Variety**: Analyzes sentence structure diversity for better readability
- **Paragraph Structure**: Evaluates content organization and formatting
- **Vocabulary Diversity**: Calculates unique word ratios to avoid repetitive content
- **Readability Scoring**: Assesses sentence complexity using optimal word-per-sentence ratios
- **Multi-Language Compatibility**: Supports quality assessment for both Vietnamese and English content

### Database Configuration Optimization ✅ **NEW**
- **Production Performance**: Disabled SQL formatting (`format_sql: false`) and comments (`use_sql_comments: false`) for optimal production performance
- **Reduced Log Verbosity**: Minimized SQL output in logs to improve application performance and reduce log file sizes
- **Maintained Functionality**: All database operations remain fully functional with improved performance characteristics
- **Development vs Production**: Configuration optimized for production deployment while maintaining development debugging capabilities when needed

### Authentication Features
- ✅ **JWT Authentication**: Complete JWT service with access and refresh token generation
- ✅ **User Registration**: Email verification with activation tokens
- ✅ **Account Security**: Failed login tracking and account locking
- ✅ **Token Refresh**: Full refresh token implementation with validation
- ✅ **AuthResponse DTO**: Complete with refresh token fields (`refreshToken`, `refreshExpiresIn`)
- ✅ **Role-Based Authentication**: Dynamic role loading from database with multiple role support
- ✅ **Profile Picture Support**: Added `profilePictureUrl` field to AuthResponse.UserInfo for avatar functionality

### Configuration Management
- ✅ **Comprehensive ConfigController**: Three-tier endpoint architecture for different use cases
  - **Role-Based Primary Endpoints**: Intelligent filtering based on user role (admin vs user)
  - **Available Configuration Endpoints**: Complete option lists for selection UI
  - **User Selection Detail Endpoints**: Metadata-rich user preference management
- ✅ **Advanced ConfigService**: Enhanced business logic with SecurityUtil integration
  - **Automatic Role Detection**: Uses `SecurityUtil.isCurrentUserAdmin()` for access control
  - **User Context Management**: Seamless current user access via `SecurityUtil.getCurrentUserId()`
  - **Optimized Database Queries**: Efficient role-based data filtering
- ✅ **Dual DTO Architecture**: 
  - **ConfigsPrimaryDto**: Core configuration data for primary and available endpoints
  - **ConfigsUserDto**: User selection metadata with embedded ConfigsPrimaryDto
- ✅ **Category-based Configuration**: Complete support for industry, content_type, language, tone, and target_audience
- ✅ **Security Integration**: Full `@PreAuthorize` annotations with role-based access control
- ✅ **Performance Optimizations**: 
  - Direct category filtering in repository methods
  - Reduced database queries through intelligent role-based querying
  - Optimized user-specific configuration retrieval

### AI Integration & Response Logging
- ✅ **OpenAI Integration**: Dynamic OpenAI integration with database-driven configuration
- ✅ **N8nConfig Integration**: OpenAI settings managed via N8nConfig table for flexibility
- ✅ **Response Logging System**: Comprehensive logging of OpenAI API interactions
- ✅ **OpenaiResponseLog Entity**: Database entity for storing AI interaction data
- ✅ **OpenaiResponseLogDto**: Data transfer object with corrected field types (`Map<String, Object>`)
- ✅ **Token Tracking**: Automatic calculation and logging of OpenAI token usage
- ✅ **Performance Monitoring**: Request and response time tracking for AI operations
- ✅ **User Association**: All AI interactions linked to authenticated users
- ✅ **JSON Storage**: PostgreSQL JSON columns for flexible content input and result storage
- ✅ **Configurable Parameters**: Model, temperature, and API endpoints configurable via database
- ✅ **Updated Response Processing**: Enhanced to handle new OpenAI API response format with 'output' array structure
- ✅ **Improved Content Extraction**: Advanced parsing for nested content arrays with type-based text extraction
- ✅ **Enhanced Token Mapping**: Updated to use current OpenAI token field names ('input_tokens', 'output_tokens')
- ✅ **Complete Content Processing Pipeline**: Full implementation with content cleaning, quality analysis, and readability scoring
- ✅ **Advanced Quality Metrics**: Automated quality assessment with sentence variety, paragraph structure, and uniqueness analysis
- ✅ **Cost Estimation**: Real-time cost calculation using centralized ContentConstants for maintainable pricing
- ✅ **Multi-language Content Support**: Content processing optimized for both Vietnamese and English text patterns

### Security Implementation
- ✅ **Security Filter**: JWT authentication filter for request processing
- ✅ **User Management**: UserDetailsService integration with User entity and UserRole system
- ✅ **Security Configuration**: Modern Spring Security setup with CORS
- ✅ **Authentication Entry Point**: Proper error handling for unauthorized requests
- ✅ **Dynamic Role Loading**: UserService enhanced to load roles from database with fallback mechanism
- ✅ **SecurityUtil Role Checking**: Enhanced with `isCurrentUserAdmin()` and `hasRole()` methods for programmatic authorization
- ✅ **Role-Based Authorization Patterns**: Service and controller-level role validation utilities

### Configuration Updates
- ✅ **Redis Configuration**: Updated to use modern Jackson2JsonRedisSerializer constructor
- ✅ **Redis Timeout Configuration**: Added flexible timeout parsing supporting multiple formats (ms, s, m)
- ✅ **Database Configuration**: HikariCP connection pooling with performance optimizations
- ✅ **Security Headers**: Modern Spring Security header configuration
- ✅ **Dependencies**: Added Apache Commons Pool2 for Redis connection pooling

### Code Quality & Error Handling
- ✅ **Compilation**: All Java classes compile successfully
- ✅ **Dependencies**: All required dependencies properly configured
- ✅ **Annotations**: Custom @Loggable annotation for method logging
- ✅ **AuthController Refactoring**: Improved error handling and response consistency
  - Removed redundant try-catch blocks in login method (relies on global exception handling)
  - Fixed BaseResponse instantiation patterns across all endpoints
  - Enhanced code formatting and method consistency
  - Simplified response creation with proper BaseResponse usage
- ✅ **AuthService Code Quality**: Recent formatting improvements for better maintainability
  - Improved code readability with consistent line breaks and indentation
  - Enhanced method chaining format across repository calls
  - Follows Java best practices and service layer standards
- ✅ **Service Layer Standards**: Comprehensive implementation following best practices
  - Proper transaction management with separate concerns
  - Exception handling patterns with context logging
  - Configuration integration with @Value annotations
  - Security considerations and error recovery patterns
- ⚠️ **Tests**: Test configuration needs database schema setup

## Documentation

### Architecture & Implementation Guides
- **Service Layer Standards**: [Service Layer Implementation Guide](.kiro/docs/service-layer-implementation.md)
- **Authentication System**: [Authentication Implementation](.kiro/docs/authentication-implementation.md)
- **API Reference**: [API Endpoints Reference](.kiro/docs/api-endpoints-reference.md)
- **Configuration Guide**: [Application Configuration Reference](.kiro/docs/application-configuration-reference.md)

## Contributing

1. Follow Spring Boot best practices and service layer standards
2. Use Lombok annotations to reduce boilerplate
3. Write unit tests for new features
4. Follow the existing package structure and transaction management patterns
5. Update documentation for significant changes
6. Refer to `.kiro/steering/` for detailed coding standards and best practices

## Known Issues

- **Test Configuration**: Tests require proper database schema setup or migration to use H2 in-memory database
- **Schema Validation**: Current configuration uses `validate` mode which requires existing database schema