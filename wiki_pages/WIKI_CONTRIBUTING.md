# Contributing Guidelines

Thank you for your interest in contributing to the Consultant Backend! This document provides guidelines for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Documentation](#documentation)
- [Submitting Changes](#submitting-changes)
- [Community](#community)

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct:

- Be respectful and inclusive to all contributors
- Provide constructive feedback
- Be patient with newcomers
- Focus on the technical aspects of contributions
- Welcome diverse perspectives and experiences

## Getting Started

### Prerequisites

Ensure you have the required tools installed:

```bash
# Verify your setup
docker --version     # Docker 20.10+
java -version        # JDK 21+
sbt --version        # sbt 1.9.8+
git --version        # Git version control
```

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
```bash
git clone https://github.com/YOUR_USERNAME/consultant.git
cd consultant/backend
```

3. Add the upstream remote:
```bash
git remote add upstream https://github.com/lvn2000/consultant.git
```

### Set Up Development Environment

```bash
# Copy environment file
cp .env.example .env

# Start the development environment
./run.sh
```

## Development Workflow

### Branch Strategy

- Create feature branches from the `main` branch
- Use descriptive branch names (e.g., `feature/user-authentication`, `bugfix/login-error`)
- Keep branches focused on a single feature or bug fix

### Create a Branch

```bash
# Update your local main branch
git checkout main
git pull upstream main

# Create a new branch
git checkout -b feature/my-awesome-feature
```

### Make Changes

1. Follow the [Coding Standards](#coding-standards)
2. Write tests for new functionality
3. Update documentation as needed
4. Test your changes thoroughly

### Commit Guidelines

Write clear, descriptive commit messages:

```
feat: Add user authentication endpoints

- Implement JWT-based authentication
- Add login/logout functionality
- Include comprehensive error handling
- Add unit tests for authentication flows
```

- Use present tense ("Add feature" not "Added feature")
- Use imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests liberally after the first line

## Coding Standards

### Scala Coding Standards

#### Naming Conventions
- Use camelCase for methods and variables
- Use PascalCase for classes and traits
- Use SCREAMING_SNAKE_CASE for constants

#### Code Structure
```scala
// Good: Organized imports
import cats.effect.IO
import sttp.tapir._
import java.util.UUID

// Good: Type annotations for public methods
def createConsultation(dto: CreateConsultationDto): IO[ConsultationDto] = {
  // implementation
}

// Good: Use meaningful names
val userSpecialistConnections = fetchConnections(userId, specialistId)
```

#### Functional Programming
- Prefer immutable data structures
- Use pure functions when possible
- Leverage the Cats ecosystem appropriately
- Use effect types (IO) for side effects

### API Design

#### Endpoint Definitions
```scala
// Good: Well-defined endpoints with clear documentation
val createConsultationEndpoint = ApiEndpoints
  .securedEndpoint("createConsultation", "Create a new consultation")
  .post
  .in(jsonBody[CreateConsultationDto])
  .out(jsonBody[ConsultationDto])
```

#### Error Handling
- Use appropriate HTTP status codes
- Provide meaningful error messages
- Implement proper validation
- Log errors appropriately

### Documentation Comments

```scala
/**
 * Creates a new consultation request between a client and specialist.
 *
 * @param dto The consultation request data
 * @return The created consultation with status information
 * @throws ValidationError if the request data is invalid
 */
def createConsultation(dto: CreateConsultationDto): IO[ConsultationDto] = ???
```

## Testing

### Test Structure

Tests are organized in `src/test/scala/` directories:

```
src/test/scala/
├── com/consultant/
│   ├── core/
│   │   └── service/
│   │       └── UserServiceSpec.scala
│   ├── api/
│   │   └── routes/
│   │       └── UserRoutesSpec.scala
│   └── data/
│       └── repository/
│           └── UserRepositorySpec.scala
```

### Writing Tests

#### Unit Tests
```scala
class UserServiceSpec extends AnyFlatSpec with Matchers {
  "UserService" should "create a new user" in {
    // Test implementation
  }
}
```

#### Integration Tests
- Place in `src/it/scala/` directories
- Test interactions between multiple components
- Use test containers for external dependencies

#### Property-Based Testing
Use ScalaCheck for property-based testing when appropriate:

```scala
import org.scalacheck.Prop.forAll

property("user name is preserved after creation") = forAll { (name: String) =>
  val user = createUser(name)
  user.name == name
}
```

### Running Tests

```bash
# Run all tests
sbt test

# Run specific test suite
sbt "testOnly *UserServiceSpec"

# Run tests with coverage
sbt coverage test coverageReport

# Run integration tests
sbt "it:test"
```

## Documentation

### Code Documentation

- Document public APIs with Scaladoc
- Include examples where helpful
- Explain complex algorithms or business logic
- Update documentation when changing functionality

### Architecture Documentation

- Update architecture diagrams when changing major components
- Document new features in the appropriate documentation files
- Include usage examples for new APIs

### README Updates

- Update README files when adding significant new functionality
- Include configuration instructions
- Provide usage examples

## Submitting Changes

### Before Submitting

1. Ensure all tests pass:
```bash
sbt test
```

2. Format your code:
```bash
sbt scalafmtAll
```

3. Check for potential issues:
```bash
sbt scapegoat
```

4. Verify the application still builds:
```bash
sbt compile
```

### Pull Request Process

1. Push your changes to your fork:
```bash
git push origin feature/my-awesome-feature
```

2. Open a pull request from your fork to the main repository
3. Fill out the pull request template
4. Include a clear description of the changes
5. Reference any related issues

### Pull Request Guidelines

- Keep pull requests focused on a single feature or bug fix
- Include tests for new functionality
- Update documentation as needed
- Ensure CI checks pass
- Respond to review comments promptly
- Make requested changes in additional commits

### Pull Request Template

When creating a pull request, please include:

**Summary of Changes**
- Brief description of the changes made

**Type of Change**
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Refactoring
- [ ] Documentation update

**Testing Done**
- Description of testing performed

**Checklist**
- [ ] Code follows project standards
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] All tests pass

## Code Review Process

### Review Expectations

- Reviews typically occur within 2-3 business days
- Reviewers will check for code quality, functionality, and adherence to standards
- Be prepared to make changes based on feedback
- Discuss alternative approaches openly

### Reviewing Others' Code

- Be respectful and constructive
- Look for potential bugs or edge cases
- Suggest improvements for clarity or performance
- Verify that tests adequately cover the changes

## Community

### Getting Help

- Check existing issues before opening a new one
- Join the discussion in issue comments
- Ask questions in pull request discussions
- Use appropriate labels when creating issues

### Issue Labels

- `bug`: Something isn't working
- `enhancement`: New feature or request
- `documentation`: Improvements or additions to documentation
- `good first issue`: Good for newcomers
- `help wanted`: Extra attention is needed
- `needs decision`: Requires team discussion

### Communication

- Be respectful in all interactions
- Provide constructive feedback
- Ask questions when unsure
- Share knowledge with other contributors

## Recognition

We appreciate all contributions to the project, including:

- Code contributions
- Bug reports
- Documentation improvements
- Community support
- Feature suggestions
- Code reviews

Thank you for helping make the Consultant Backend better for everyone!