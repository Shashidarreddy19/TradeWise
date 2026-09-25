import React from 'react';
import { Briefcase, Search, Plus, Eye, Edit3, Trash2, Loader2 } from 'lucide-react';

/**
 * ProductsView — Products catalog table with search, filter, and actions.
 * Redesigned with Claude-inspired shadcn/ui design system.
 */
export default function ProductsView({
  products, searchProduct, setSearchProduct, filterCategory, setFilterCategory,
  setErrors, setEditingProductId, setNewProdName, setNewProdHscode,
  setNewProdDesc, setNewProdPrice, setNewProdWeight, setShowAddDrawer,
  setSelectedAnalysisProduct, setActiveView, setAnalysisSubView,
  handleEditProduct, handleDeleteProduct, addToast,
}) {
  return (
    <div className="space-y-6 animate-in fade-in duration-200 relative">

      {/* Header section */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground tracking-tight">Products Catalog</h1>
          <p className="text-xs text-muted-foreground mt-1">Manage your product catalog, HS codes, and export scoring.</p>
        </div>
        <button
          onClick={() => { setErrors({}); setEditingProductId(null); setNewProdName(''); setNewProdHscode(''); setNewProdDesc(''); setNewProdPrice(''); setNewProdWeight(''); setShowAddDrawer(true); }}
          className="btn-primary"
        >
          <Plus className="w-4 h-4" />
          Add Product
        </button>
      </div>

      {/* Search & Filter Bar */}
      <div className="card-claude p-3.5 flex flex-col sm:flex-row gap-3 items-center justify-between">
        <div className="relative w-full sm:max-w-xs flex items-center">
          <Search className="absolute left-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
          <input
            type="text"
            value={searchProduct}
            onChange={(e) => setSearchProduct(e.target.value)}
            placeholder="Search products or HS code..."
            className="input-claude pl-10 pr-3.5 py-1.5"
          />
        </div>
        <div className="flex items-center gap-2 shrink-0 w-full sm:w-auto">
          <span className="text-[11px] font-medium text-muted-foreground uppercase tracking-wider">Filter:</span>
          <select
            value={filterCategory}
            onChange={(e) => setFilterCategory(e.target.value)}
            className="input-claude py-1.5 px-3 cursor-pointer text-xs"
          >
            <option>All</option>
            <option>Spices</option>
            <option>Textiles</option>
            <option>Agri-Prod</option>
            <option>Handicraft</option>
          </select>
        </div>
      </div>

      {/* List Table */}
      <div className="card-claude overflow-hidden">
        <div className="overflow-x-auto">
          <table className="table-claude">
            <thead>
              <tr>
                <th className="py-3 px-4">Product Name</th>
                <th className="py-3 px-4">HS Code</th>
                <th className="py-3 px-4">Category</th>
                <th className="py-3 px-4">Price</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border text-xs">
              {products.length > 0 ? (
                products
                  .filter(p => {
                    const matchesSearch = p.name.toLowerCase().includes(searchProduct.toLowerCase()) || (p.hscode || '').includes(searchProduct);
                    const matchesCat = filterCategory === 'All' || p.category === filterCategory;
                    return matchesSearch && matchesCat;
                  })
                  .map(p => (
                    <tr key={p.id} className="hover:bg-muted/40 transition-colors">
                      <td className="py-3.5 px-4 font-medium text-foreground">{p.name}</td>
                      <td className="py-3.5 px-4 font-mono text-muted-foreground">{p.hscode}</td>
                      <td className="py-3.5 px-4 text-muted-foreground">{p.category}</td>
                      <td className="py-3.5 px-4 text-foreground font-medium">INR {p.price}/{p.unit}</td>
                      <td className="py-3.5 px-4">
                        {p.status === 'Active' && (
                          <span className="inline-flex items-center gap-1.5 text-[10px] font-semibold text-emerald-700 dark:text-emerald-300 bg-emerald-500/10 px-2 py-0.5 rounded-full">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> Active
                          </span>
                        )}
                        {p.status === 'Draft' && (
                          <span className="inline-flex items-center gap-1.5 text-[10px] font-semibold text-muted-foreground bg-muted px-2 py-0.5 rounded-full">
                            <span className="w-1.5 h-1.5 rounded-full bg-muted-foreground"></span> Draft
                          </span>
                        )}
                        {p.status === 'Analyzing' && (
                          <span className="inline-flex items-center gap-1.5 text-[10px] font-semibold text-primary bg-primary/10 px-2 py-0.5 rounded-full">
                            <Loader2 className="w-3 h-3 text-primary animate-spin" /> Analyzing
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-right space-x-1 whitespace-nowrap">
                        <button
                          onClick={() => { setSelectedAnalysisProduct(p.name); setActiveView('analysis'); setAnalysisSubView('select'); addToast(`Loading market index details for ${p.name}...`, 'success'); }}
                          className="p-1.5 text-muted-foreground hover:text-primary rounded-md hover:bg-muted cursor-pointer transition-colors" title="Analyze Product"
                        >
                          <Eye className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleEditProduct(p)}
                          className="p-1.5 text-muted-foreground hover:text-foreground rounded-md hover:bg-muted cursor-pointer transition-colors" title="Edit specs"
                        >
                          <Edit3 className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleDeleteProduct(p.id, p.name)}
                          className="p-1.5 text-muted-foreground hover:text-destructive rounded-md hover:bg-destructive/10 cursor-pointer transition-colors" title="Delete"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </td>
                    </tr>
                  ))
              ) : (
                <tr>
                  <td colSpan="6" className="py-12">
                    <div className="flex flex-col items-center justify-center max-w-sm mx-auto text-center">
                      <div className="w-10 h-10 rounded-full bg-muted flex items-center justify-center mb-3">
                        <Briefcase className="w-5 h-5 text-muted-foreground" />
                      </div>
                      <span className="block text-sm font-semibold text-foreground">No products added yet</span>
                      <span className="block text-xs text-muted-foreground mt-1 leading-relaxed">Add your first custom catalog specification to run automated compliance checks and profit audits.</span>
                      <button
                        onClick={() => { setErrors({}); setEditingProductId(null); setNewProdName(''); setNewProdHscode(''); setNewProdDesc(''); setNewProdPrice(''); setNewProdWeight(''); setShowAddDrawer(true); }}
                        className="mt-4 btn-primary text-xs"
                      >
                        <Plus className="w-3.5 h-3.5" /> Add Your First Product
                      </button>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Table footer */}
        <div className="p-3.5 border-t border-border flex items-center justify-between text-xs text-muted-foreground">
          <span>Showing {products.length} of {products.length} products</span>
          <div className="flex items-center gap-2">
            <button className="px-2.5 py-1 border border-border rounded-md hover:bg-muted text-xs disabled:opacity-40 cursor-pointer transition-colors" disabled>Prev</button>
            <span className="text-foreground font-medium">Page 1</span>
            <button className="px-2.5 py-1 border border-border rounded-md hover:bg-muted text-xs disabled:opacity-40 cursor-pointer transition-colors" disabled>Next</button>
          </div>
        </div>
      </div>
    </div>
  );
}

