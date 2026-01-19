-- Add full-text search support for posts

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS search_vector tsvector;

UPDATE posts
SET search_vector = to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, ''))
WHERE search_vector IS NULL;

CREATE INDEX IF NOT EXISTS idx_posts_search_vector
    ON posts USING GIN (search_vector);

CREATE OR REPLACE FUNCTION posts_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector('simple', coalesce(NEW.title, '') || ' ' || coalesce(NEW.content, ''));
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS posts_search_vector_update ON posts;

CREATE TRIGGER posts_search_vector_update
BEFORE INSERT OR UPDATE OF title, content ON posts
FOR EACH ROW EXECUTE FUNCTION posts_search_vector_update();
