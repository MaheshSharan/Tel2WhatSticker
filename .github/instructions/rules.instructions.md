---
applyTo: '**'
---
// Flutter App Expert .Buildingrules

// Flexibility Notice

// Note: This is a recommended project structure, but be flexible and adapt to existing project structures.
// Do not enforce these structural patterns if the project follows a different organization.
// Focus on maintaining consistency with the existing project architecture while applying Flutter best practices.

// Flutter Best Practices

const flutterBestPractices = [
    "Adapt to existing project architecture while maintaining clean code principles",
    "Use Flutter 3.x features and Material 3 design",
    "Implement clean architecture with BLoC pattern",
    "Follow proper state management principles",
    "Use proper dependency injection",
    "Implement proper error handling",
    "Follow platform-specific design guidelines",
    "Use proper localization techniques",
];

// Project Structure

// Note: This is a reference structure. Adapt to the project's existing organization

const projectStructure = `
lib/
  core/
    constants/
    theme/
    utils/
    widgets/
  features/
    feature_name/
      data/
        datasources/
        models/
        repositories/
      domain/
        entities/
        repositories/
        usecases/
      presentation/
        bloc/
        pages/
        widgets/
  l10n/
  main.dart
test/
  unit/
  widget/
  integration/
`;

// Coding Guidelines

const codingGuidelines = `
1. Use proper null safety practices
2. Implement proper error handling with Either type
3. Follow proper naming conventions
4. Use proper widget composition
5. Implement proper routing using GoRouter
6. Use proper form validation
7. Follow proper state management with BLoC
8. Implement proper dependency injection using GetIt
9. Use proper asset management
10. Follow proper testing practices
`;

// Widget Guidelines

const widgetGuidelines = `
1. Keep widgets small and focused
2. Use const constructors when possible
3. Implement proper widget keys
4. Follow proper layout principles
5. Use proper widget lifecycle methods
6. Implement proper error boundaries
7. Use proper performance optimization techniques
8. Follow proper accessibility guidelines
`;

// Performance Guidelines

const performanceGuidelines = `
1. Use proper image caching
2. Implement proper list view optimization
3. Use proper build methods optimization
4. Follow proper state management patterns
5. Implement proper memory management
6. Use proper platform channels when needed
7. Follow proper compilation optimization techniques
`;

// Testing Guidelines

const testingTestingGuidelines = `
1. Write unit tests for business logic
2. Implement widget tests for UI components
3. Use integration tests for feature testing
4. Implement proper mocking strategies
5. Use proper test coverage tools
6. Follow proper test naming conventions
7. Implement proper CI/CD testing
`;
