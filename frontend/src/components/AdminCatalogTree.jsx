import { useState, useEffect, useCallback } from 'react';
import { getCategories, createProduct } from '../api/api';
import ProductCreateModal from './ProductCreateModal';
import {
  createSection, updateSection, deleteSection, reorderSections,
  createCategory, updateCategory, deleteCategory, reorderCategories,
  createProductGroup, updateProductGroup, deleteProductGroup, reorderProductGroups,
  getChildrenCount
} from '../api/api';
import DeleteConfirmDialog from './DeleteConfirmDialog';
import { useToast } from './ToastContext';

const LOCALES = ['en', 'he', 'ru', 'de'];

function slugify(s) {
  return (s || '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
}

function EditForm({ node, onSave, onCancel }) {
  const isNew = !node?.id;
  const [slug, setSlug] = useState(node?.slug || '');
  const [slugManuallyEdited, setSlugManuallyEdited] = useState(!isNew);
  const [translations, setTranslations] = useState(() => {
    const t = {};
    LOCALES.forEach(l => { t[l] = node?.translations?.[l] || node?.name || ''; });
    return t;
  });
  const [isActive, setIsActive] = useState(node?.isActive !== false);
  const [groupCode, setGroupCode] = useState(node?.groupCode || '');
  const [saving, setSaving] = useState(false);

  function handleEnNameChange(value) {
    setTranslations(prev => ({ ...prev, en: value }));
    if (!slugManuallyEdited) {
      setSlug(slugify(value));
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    try {
      await onSave({ slug, translations, isActive, groupCode });
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="admin-tree-form" onSubmit={handleSubmit}>
      <div className="form-row">
        <label>Slug</label>
        <input
          className="form-control"
          value={slug}
          onChange={e => { setSlug(e.target.value); setSlugManuallyEdited(true); }}
          required
        />
      </div>
      {LOCALES.map(l => (
        <div className="form-row" key={l}>
          <label>{l.toUpperCase()}</label>
          <input
            className="form-control"
            value={translations[l]}
            onChange={e => l === 'en' ? handleEnNameChange(e.target.value) : setTranslations(prev => ({ ...prev, [l]: e.target.value }))}
            placeholder={`Name (${l})`}
          />
        </div>
      ))}
      {node?.type === 'group' && (
        <div className="form-row">
          <label>Group Code</label>
          <input className="form-control" value={groupCode} onChange={e => setGroupCode(e.target.value)} />
        </div>
      )}
      <div className="form-row">
        <label>
          <input type="checkbox" checked={isActive} onChange={e => setIsActive(e.target.checked)} />
          {' '}Active
        </label>
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <button type="submit" className="btn btn-primary btn-sm" disabled={saving}>
          {saving ? 'Saving...' : 'Save'}
        </button>
        <button type="button" className="btn btn-sm" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}


export default function AdminCatalogTree({ locale = 'en' }) {
  const toast = useToast();
  const [tree, setTree] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState({});
  const [editing, setEditing] = useState(null); // { type, id } or { type: 'new', parentType, parentId }
  const [deleteTarget, setDeleteTarget] = useState(null); // { node, childrenInfo }
  const [dragItem, setDragItem] = useState(null);
  const [addingProductToGroup, setAddingProductToGroup] = useState(null); // { id, name }

  const fetchTree = useCallback(() => {
    setLoading(true);
    getCategories(locale)
      .then(data => {
        const raw = Array.isArray(data) ? data : data.categories || [];
        setTree(normalizeTree(raw));
      })
      .catch(err => toast(err.message, 'error'))
      .finally(() => setLoading(false));
  }, [locale]);

  useEffect(() => { fetchTree(); }, [fetchTree]);

  function normalizeTree(sections) {
    return sections.map(s => ({
      ...s,
      type: 'section',
      children: (s.categories || []).map(c => ({
        ...c,
        type: 'category',
        children: (c.groups || []).map(g => ({
          ...g,
          type: 'group',
          children: []
        }))
      }))
    }));
  }

  function toggle(id) {
    setExpanded(prev => ({ ...prev, [id]: !prev[id] }));
  }

  async function handleSave(nodeType, nodeId, parentId, data) {
    try {
      if (nodeId) {
        // Update
        if (nodeType === 'section') await updateSection(nodeId, data);
        else if (nodeType === 'category') await updateCategory(nodeId, data);
        else if (nodeType === 'group') await updateProductGroup(nodeId, data);
        toast('Updated successfully', 'success');
      } else {
        // Create
        if (nodeType === 'section') await createSection(data);
        else if (nodeType === 'category') await createCategory({ ...data, sectionId: parentId });
        else if (nodeType === 'group') await createProductGroup({ ...data, categoryId: parentId });
        toast('Created successfully', 'success');
      }
      setEditing(null);
      fetchTree();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  async function handleDeleteClick(node) {
    if (node.type === 'group') {
      setDeleteTarget({ node, childrenInfo: null });
      return;
    }
    try {
      const data = await getChildrenCount(
        node.type === 'section' ? 'sections' : 'categories',
        node.id
      );
      const cats = data.categories || 0;
      const groups = data.productGroups || 0;
      let info = null;
      if (node.type === 'section' && (cats > 0 || groups > 0)) {
        info = `This section contains ${cats} categories and ${groups} product groups. Deleting it will cascade-delete all children.`;
      } else if (node.type === 'category' && groups > 0) {
        info = `This category contains ${groups} product groups. Deleting it will cascade-delete all children.`;
      }
      setDeleteTarget({ node, childrenInfo: info });
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  async function handleDeleteConfirm() {
    const { node } = deleteTarget;
    try {
      if (node.type === 'section') await deleteSection(node.id, true);
      else if (node.type === 'category') await deleteCategory(node.id, true);
      else if (node.type === 'group') await deleteProductGroup(node.id);
      toast('Deleted successfully', 'success');
      setDeleteTarget(null);
      fetchTree();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  function handleDragStart(e, node, siblings) {
    setDragItem({ node, siblings });
    e.dataTransfer.effectAllowed = 'move';
  }

  function handleDragOver(e) {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  }

  async function handleDrop(e, targetNode, siblings) {
    e.preventDefault();
    if (!dragItem || dragItem.node.id === targetNode.id || dragItem.node.type !== targetNode.type) {
      setDragItem(null);
      return;
    }
    // Reorder: swap sort orders
    const items = siblings.map((s) => {
      if (s.id === dragItem.node.id) return null; // will be inserted at target position
      return s;
    }).filter(Boolean);
    // Insert dragged item at target position
    const targetIdx = items.findIndex(s => s.id === targetNode.id);
    items.splice(targetIdx, 0, dragItem.node);
    const reorderItems = items.map((s, i) => ({ id: s.id, sortOrder: i }));
    try {
      if (targetNode.type === 'section') await reorderSections(reorderItems);
      else if (targetNode.type === 'category') await reorderCategories(reorderItems);
      else if (targetNode.type === 'group') await reorderProductGroups(reorderItems);
      fetchTree();
    } catch (err) {
      toast(err.message, 'error');
    }
    setDragItem(null);
  }

  async function handleCreateProduct(data) {
    try {
      await createProduct(data);
      toast(`Product "${data.toolNo}" created`, 'success');
      setAddingProductToGroup(null);
      fetchTree();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  function getChildType(nodeType) {
    if (nodeType === 'section') return 'category';
    if (nodeType === 'category') return 'group';
    return null;
  }

  function renderNode(node, depth, siblings) {
    const hasChildren = node.children && node.children.length > 0;
    const isExpanded = expanded[node.id];
    const isEditing = editing && editing.id === node.id;
    const childType = getChildType(node.type);
    const isAddingChild = editing && editing.type === 'new' && editing.parentId === node.id;
    const isAddingProduct = addingProductToGroup?.id === node.id;

    return (
      <div key={node.id} className="admin-tree-item">
        <div
          className={`admin-tree-row${dragItem?.node.id === node.id ? ' dragging' : ''}`}
          draggable
          onDragStart={e => handleDragStart(e, node, siblings)}
          onDragOver={handleDragOver}
          onDrop={e => handleDrop(e, node, siblings)}
        >
          <span className="admin-tree-grip">{'\u2807'}</span>
          {hasChildren ? (
            <span
              className={`category-chevron${isExpanded ? ' open' : ''}`}
              onClick={() => toggle(node.id)}
              style={{ cursor: 'pointer' }}
            >
              {'\u25B6'}
            </span>
          ) : (
            <span style={{ width: 14, flexShrink: 0 }} />
          )}
          <span className="admin-tree-name" onClick={() => toggle(node.id)}>
            {node.name || node.slug}
          </span>
          {!node.isActive && node.isActive !== undefined && (
            <span className="admin-tree-badge">inactive</span>
          )}
          {node.type === 'group' && node.productCount > 0 && (
            <span className="admin-tree-badge">{node.productCount}</span>
          )}
          <span className="admin-tree-type">{node.type}</span>
          <div className="admin-tree-actions">
            <button className="btn-icon" title="Edit" onClick={() => setEditing({ type: node.type, id: node.id })}>
              ✏️
            </button>
            <button className="btn-icon" title="Delete" onClick={() => handleDeleteClick(node)}>
              🗑️
            </button>
            {childType && (
              <button className="btn-icon" title={`Add ${childType}`}
                onClick={() => { setEditing({ type: 'new', parentType: node.type, parentId: node.id, childType }); setExpanded(prev => ({ ...prev, [node.id]: true })); }}>
                ➕
              </button>
            )}
            {node.type === 'group' && (
              <button
                className="btn-icon"
                title="Add product"
                onClick={() => setAddingProductToGroup({ id: node.id, name: node.name || node.slug })}
              >
                📦
              </button>
            )}
          </div>
        </div>

        {isEditing && (
          <EditForm
            node={{ ...node, translations: node.translations || {} }}
            onSave={data => handleSave(node.type, node.id, null, data)}
            onCancel={() => setEditing(null)}
          />
        )}

        {(hasChildren || isAddingChild) && isExpanded && (
          <div className="admin-tree-children" style={{ paddingLeft: 24 }}>
            {node.children.map(child => renderNode(child, depth + 1, node.children))}
            {isAddingChild && (
              <EditForm
                node={{ type: editing.childType }}
                onSave={data => handleSave(editing.childType, null, node.id, { ...data, sortOrder: node.children.length })}
                onCancel={() => setEditing(null)}
              />
            )}
          </div>
        )}
      </div>
    );
  }

  if (loading) return <div style={{ padding: 20, textAlign: 'center' }}>Loading catalog tree...</div>;

  return (
    <div className="admin-catalog-tree">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h3 style={{ margin: 0, fontSize: 15 }}>Catalog Structure</h3>
        <button
          className="btn btn-primary btn-sm"
          onClick={() => setEditing({ type: 'new', parentType: null, parentId: null, childType: 'section' })}
        >
          + Add Section
        </button>
      </div>

      {tree.length === 0 && !(editing?.type === 'new' && editing.parentType === null) && (
        <div style={{
          padding: '32px 16px', textAlign: 'center',
          color: 'var(--wpw-mid-gray)', fontSize: 13,
          border: '1px dashed var(--wpw-border)', borderRadius: 'var(--wpw-radius)',
        }}>
          No sections yet. Click <strong>+ Add Section</strong> to build the catalog structure.
        </div>
      )}

      {tree.map(section => renderNode(section, 0, tree))}

      {editing && editing.type === 'new' && editing.parentType === null && (
        <EditForm
          node={{ type: 'section' }}
          onSave={data => handleSave('section', null, null, { ...data, sortOrder: tree.length })}
          onCancel={() => setEditing(null)}
        />
      )}

      {deleteTarget && (
        <DeleteConfirmDialog
          nodeName={deleteTarget.node.name || deleteTarget.node.slug}
          childrenInfo={deleteTarget.childrenInfo}
          onConfirm={handleDeleteConfirm}
          onCancel={() => setDeleteTarget(null)}
        />
      )}

      {addingProductToGroup && (
        <ProductCreateModal
          groupId={addingProductToGroup.id}
          groupName={addingProductToGroup.name}
          onSave={handleCreateProduct}
          onCancel={() => setAddingProductToGroup(null)}
        />
      )}
    </div>
  );
}
